package me.szabee.doubledoors.bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import me.szabee.doubledoors.bukkit.api.DoubleDoorsAPI;
import me.szabee.doubledoors.bukkit.config.ClaimSettings;
import me.szabee.doubledoors.bukkit.config.PlayerPreferences;
import me.szabee.doubledoors.bukkit.config.PluginConfig;
import me.szabee.doubledoors.bukkit.i18n.TranslationCatalog;
import me.szabee.doubledoors.bukkit.i18n.TranslationManager;
import me.szabee.doubledoors.bukkit.listeners.DoorCacheInvalidationListener;
import me.szabee.doubledoors.bukkit.listeners.DoorInteractListener;
import me.szabee.doubledoors.bukkit.listeners.RedstoneListener;
import me.szabee.doubledoors.bukkit.migration.YamlToSqlMigrator;
import me.szabee.doubledoors.bukkit.storage.BukkitSharedSqlStorage;
import me.szabee.doubledoors.bukkit.util.DoorUtil;
import me.szabee.doubledoors.bukkit.util.FastStatsManager;
import me.szabee.doubledoors.bukkit.util.OpenableType;
import me.szabee.doubledoors.bukkit.util.PluginUpdaterManager;
import me.szabee.doubledoors.bukkit.util.ProtectionCompat;
import me.szabee.doubledoors.bukkit.util.SchedulerBridge;
import me.szabee.doubledoors.bukkit.version.VersionBridge;
import me.szabee.doubledoors.storage.SharedSqlStorage;
import me.szabee.doubledoors.util.TaskToken;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Door;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for DoubleDoors.
 */
public final class DoubleDoors extends JavaPlugin {

  private static final String LOCALE_PERMISSION = "doubledoors.locale";

  private volatile PluginConfig pluginConfig;
  private volatile PlayerPreferences playerPreferences;
  private volatile ClaimSettings claimSettings;
  private volatile TranslationManager translationManager;
  private volatile SharedSqlStorage sqlStorage;
  private final FastStatsManager fastStats = new FastStatsManager(this);
  private final PluginUpdaterManager pluginUpdater = new PluginUpdaterManager(this);
  private volatile TaskToken proxyBridgePollTask;
  private final Set<UUID> debugPlayers = ConcurrentHashMap.newKeySet();
  private final Set<Material> customOpenables = ConcurrentHashMap.newKeySet();
  private volatile boolean localGeyserBridgeAvailable;
  private volatile boolean geyserBridgeAvailable;
  private volatile boolean geyserBridgeLogged;
  private volatile VersionBridge versionBridge;
  private volatile int initGeneration = 0;
  private final DoubleDoorsAPI api = new DoubleDoorsAPI() {
    /**
     * Returns whether linked open behavior is available to a player.
     *
     * @param player the player to check
     * @return {@code true} when the server, player preference, and permission allow linked opening
     */
    @Override
    public boolean isDoubleBehaviorEnabled(Player player) {
      return (
        player != null &&
        pluginConfig.isServerWideEnabled() &&
        isEnabledForPlayer(player) &&
        player.hasPermission("doubledoors.use")
      );
    }

    /**
     * Opens or closes blocks linked to the origin block.
     *
     * @param origin the block that initiated the linked open action
     * @param actor the player responsible for the action, or {@code null} for non-player triggers
     * @return {@code true} when at least one linked block was changed
     */
    @Override
    public boolean triggerLinkedOpen(Block origin, Player actor) {
      if (!isApiTriggerAllowed(origin, actor)) {
        return false;
      }

      BlockData data = origin.getBlockData();
      if (!(data instanceof Openable openable)) {
        return false;
      }

      boolean targetState = !openable.isOpen();
      if (data instanceof Door) {
        DoorUtil.MirrorSearchResult search =
          DoorUtil.analyzeMirroredDoubleDoorPartner(origin);
        if (!search.found()) {
          search = DoorUtil.analyzeCornerDoorPartner(origin);
        }
        if (!search.found()) {
          return false;
        }
        Block partner = search.partner();
        if (!isLocationAllowed(partner)) {
          return false;
        }
        if (actor != null) {
          String denyReason = ProtectionCompat.explainLinkedDoorDeniedReason(
            DoubleDoors.this,
            actor,
            partner
          );
          if (!denyReason.isEmpty()) {
            return false;
          }
        }

        BlockData partnerData = partner.getBlockData();
        if (!(partnerData instanceof Openable linked)) {
          return false;
        }
        linked.setOpen(targetState);
        partner.setBlockData(linked, false);

        Block partnerTop = partner.getRelative(BlockFace.UP);
        BlockData topData = partnerTop.getBlockData();
        if (topData instanceof Openable topOpenable) {
          topOpenable.setOpen(targetState);
          partnerTop.setBlockData(topData, false);
        }
        playLinkedFeedback(partner, OpenableType.DOOR);
        return true;
      }

      if (!pluginConfig.isEnableRecursiveOpening()) {
        return false;
      }

      var connected = DoorUtil.findConnectedDoors(
        origin,
        pluginConfig.getRecursiveOpeningMaxBlocksDistance()
      );
      if (connected.isEmpty()) {
        return false;
      }

      boolean changedAny = false;
      for (Block block : connected) {
        if (!isLocationAllowed(block)) {
          continue;
        }
        if (actor != null) {
          String denyReason = ProtectionCompat.explainLinkedDoorDeniedReason(
            DoubleDoors.this,
            actor,
            block
          );
          if (!denyReason.isEmpty()) {
            continue;
          }
        }
        BlockData linkedData = block.getBlockData();
        if (!(linkedData instanceof Openable linked)) {
          continue;
        }
        if (linked.isOpen() == targetState) {
          continue;
        }
        linked.setOpen(targetState);
        block.setBlockData(linked, false);
        OpenableType type = OpenableType.fromMaterial(block.getType());
        playLinkedFeedback(block, type == null ? OpenableType.CUSTOM : type);
        changedAny = true;
      }
      return changedAny;
    }

    /**
     * Registers a Bukkit material as a custom openable block type.
     *
     * @param material the material to treat as custom openable
     */
    @Override
    public void registerCustomOpenableBlock(Material material) {
      if (material != null) {
        customOpenables.add(material);
      }
    }

    /**
     * Removes a Bukkit material from the custom openable registry.
     *
     * @param material the material to stop treating as custom openable
     */
    @Override
    public void unregisterCustomOpenableBlock(Material material) {
      if (material != null) {
        customOpenables.remove(material);
      }
    }
  };

  /**
   * Gets the plugin configuration wrapper.
   *
   * @return the active plugin config
   */
  public PluginConfig getPluginConfig() {
    return pluginConfig;
  }

  /**
   * Gets the per-player preferences manager.
   *
   * @return the player preferences instance
   */
  public PlayerPreferences getPlayerPreferences() {
    return playerPreferences;
  }

  /**
   * Gets the per-claim settings manager.
   *
   * @return the claim settings instance
   */
  public ClaimSettings getClaimSettings() {
    return claimSettings;
  }

  /**
   * Gets the translation manager.
   *
   * @return the active translation manager
   */
  public TranslationManager getTranslationManager() {
    return translationManager;
  }

  /**
   * Gets the shared SQL storage (null when SQL mode is disabled).
   *
   * @return SQL storage or null
   */
  public SharedSqlStorage getSqlStorage() {
    return sqlStorage;
  }

  /**
   * Returns whether Geyser or Floodgate is available locally or through a shared SQL proxy heartbeat.
   *
   * @return true when Bedrock bridge support is currently detected
   */
  public boolean isGeyserBridgeAvailable() {
    return geyserBridgeAvailable;
  }

  /**
   * Gets the public DoubleDoors API for other plugins.
   *
   * @return the API surface
   */
  public DoubleDoorsAPI getApi() {
    return api;
  }

  private boolean isApiTriggerAllowed(Block origin, Player actor) {
    if (origin == null || !pluginConfig.isServerWideEnabled()) {
      return false;
    }
    if (!isLocationAllowed(origin)) {
      return false;
    }

    OpenableType type = OpenableType.fromBlockData(
      origin.getBlockData(),
      origin.getType()
    );
    if (type == null) {
      if (!isCustomOpenable(origin.getType())) {
        return false;
      }
      type = OpenableType.CUSTOM;
    }
    boolean typeEnabled = switch (type) {
      case DOOR -> pluginConfig.isEnableDoors();
      case FENCE_GATE -> pluginConfig.isEnableFenceGates();
      case TRAPDOOR -> pluginConfig.isEnableTrapdoors();
      case CUSTOM -> isCustomOpenable(origin.getType());
    };
    if (!typeEnabled) {
      return false;
    }

    if (actor == null) {
      return true;
    }

    if (!actor.hasPermission("doubledoors.use") || !isEnabledForPlayer(actor)) {
      return false;
    }

    return switch (type) {
      case DOOR -> playerPreferences.isDoorsEnabled(actor.getUniqueId());
      case FENCE_GATE -> playerPreferences.isFenceGatesEnabled(
        actor.getUniqueId()
      );
      case TRAPDOOR -> playerPreferences.isTrapdoorsEnabled(
        actor.getUniqueId()
      );
      case CUSTOM -> playerPreferences.isEnabled(actor.getUniqueId());
    };
  }

  /**
   * Checks whether the player can interact with a linked door block according to
   * active protection plugins.
   *
   * @param player the interacting player
   * @param linkedBlock the linked block to toggle
   * @return true if interaction should be allowed
   */
  public boolean canOpenLinkedDoor(Player player, Block linkedBlock) {
    return ProtectionCompat.canOpenLinkedDoor(this, player, linkedBlock);
  }

  /**
   * Returns whether a location is allowed by global location filters.
   *
   * @param block the block location to evaluate
   * @return true when allowed by configured filters
   */
  public boolean isLocationAllowed(Block block) {
    return ProtectionCompat.isLocationAllowed(this, block);
  }

  /**
   * Explains why a linked door access attempt is denied.
   *
   * @param player the acting player
   * @param linkedBlock the linked block being toggled
   * @return empty string when allowed, otherwise deny reason key
   */
  public String explainLinkedDoorDeniedReason(
    Player player,
    Block linkedBlock
  ) {
    return ProtectionCompat.explainLinkedDoorDeniedReason(
      this,
      player,
      linkedBlock
    );
  }

  /**
   * Checks whether double-door logic is globally enabled for a given player.
   *
   * @param player the player to check
   * @return true if behavior is enabled for the player
   */
  public boolean isEnabledForPlayer(Player player) {
    return playerPreferences.isEnabled(player.getUniqueId());
  }

  /**
   * Returns whether debug mode is enabled for the player.
   *
   * @param player the player to check
   * @return true when debug mode is enabled
   */
  public boolean isDebugEnabled(Player player) {
    return debugPlayers.contains(player.getUniqueId());
  }

  /**
   * Toggles debug mode for a player.
   *
   * @param player the player to toggle
   * @return true when debug is now enabled
   */
  public boolean toggleDebug(Player player) {
    UUID uuid = player.getUniqueId();
    if (debugPlayers.contains(uuid)) {
      debugPlayers.remove(uuid);
      return false;
    }
    debugPlayers.add(uuid);
    return true;
  }

  /**
   * Checks whether a material is registered as a custom openable.
   *
   * @param material the material to evaluate
   * @return true when custom-openable behavior is enabled for this material
   */
  public boolean isCustomOpenable(Material material) {
    return customOpenables.contains(material);
  }

  /**
   * Plays configured sound and particle feedback for linked blocks.
   *
   * @param linkedBlock the linked block
   * @param type the openable block type category
   */
  public void playLinkedFeedback(Block linkedBlock, OpenableType type) {
    if (pluginConfig.isPlayPartnerSound()) {
      Sound sound = pluginConfig.getPartnerSound(type);
      if (sound != null) {
        Location soundLoc = linkedBlock.getLocation().add(0.5, 0.5, 0.5);
        linkedBlock.getWorld().playSound(soundLoc, sound, 0.8f, 1.0f);
      }
    }
    if (pluginConfig.isEnablePartnerParticles()) {
      Location loc = linkedBlock.getLocation().add(0.5, 0.55, 0.5);
      linkedBlock
        .getWorld()
        .spawnParticle(
          pluginConfig.getPartnerParticle(),
          loc,
          pluginConfig.getPartnerParticleCount(),
          0.18,
          0.20,
          0.18,
          0.01
        );
    }
  }

  /**
   * Initializes the plugin, loads configuration and integrations, registers listeners,
   * and starts optional background services.
   */
  @Override
  public void onEnable() {
    saveDefaultConfig();
    pluginConfig = new PluginConfig(this);
    DoorUtil.setMirrorCacheTtlMillis(pluginConfig.getLookupCacheTtlMillis());

    versionBridge = loadVersionBridge();

    fastStats.restart(pluginConfig, geyserBridgeAvailable);
    pluginUpdater.initialize(pluginConfig);

    sqlStorage = null;
    translationManager = new TranslationManager(this);
    translationManager.reload();
    playerPreferences = new PlayerPreferences(this);
    claimSettings = new ClaimSettings(this);
    initializeSqlIfEnabledAsync();

    getServer()
      .getPluginManager()
      .registerEvents(new DoorInteractListener(this), this);
    getServer()
      .getPluginManager()
      .registerEvents(new DoorCacheInvalidationListener(), this);
    getServer()
      .getPluginManager()
      .registerEvents(new RedstoneListener(this), this);
    registerVersionListener();

    if (!registerPaperCommand()) {
      var doubledoorsCommand = getCommand("doubledoors");
      if (doubledoorsCommand != null) {
        doubledoorsCommand.setExecutor(this);
        doubledoorsCommand.setTabCompleter(this);
      }
    }

    PluginManager pluginManager = getServer().getPluginManager();
    if (pluginManager.isPluginEnabled("LuckPerms")) {
      getLogger().info(t("log.luckperms_detected"));
    }
    if (pluginManager.isPluginEnabled("GriefPrevention")) {
      getLogger().info(t("log.griefprevention_detected"));
    }
    if (pluginManager.isPluginEnabled("WorldGuard")) {
      getLogger().info(t("log.worldguard_detected"));
    }
    localGeyserBridgeAvailable = hasAnyPluginEnabled(
      pluginManager,
      "Geyser-Spigot",
      "Geyser",
      "floodgate",
      "floodgate-bukkit"
    );
    setGeyserBridgeAvailable(localGeyserBridgeAvailable);

    getLogger().info(t("log.enabled"));
    if (versionBridge != null) {
      getLogger().info(
        "Server API version: " + versionBridge.getServerApiVersion()
      );
    }
  }

  /**
   * Shuts down the plugin when the server stops.
   * <p>
   * Increments the initialization counter, then attempts to cleanly shut down
   * the FastStats metrics context if present. Any {@link RuntimeException}
   * thrown during metrics shutdown is caught and logged. Finally stops the
   * proxy bridge polling thread, disables the updater, nulls the version
   * bridge, closes player preferences, and logs the disabled message.
   */
  @Override
  public void onDisable() {
    initGeneration++;
    try {
      fastStats.shutdown();
    } finally {
      stopProxyBridgePolling();
      pluginUpdater.disable();
      versionBridge = null;
      closePlayerPreferences();
      getLogger().info(t("log.disabled"));
    }
  }

  private VersionBridge loadVersionBridge() {
    try {
      Class<?> bridgeClass = Class.forName(
        "me.szabee.doubledoors.bukkit.version.VersionBridgeImpl"
      );
      if (!VersionBridge.class.isAssignableFrom(bridgeClass)) {
        getLogger().warning(
          "Version bridge class does not implement VersionBridge."
        );
        return null;
      }
      return (VersionBridge) bridgeClass.getDeclaredConstructor().newInstance();
    } catch (ClassNotFoundException e) {
      getLogger().warning(
        "Version bridge not found; running without version-specific hooks."
      );
      return null;
    } catch (ReflectiveOperationException e) {
      getLogger().log(Level.WARNING, "Failed to initialize version bridge.", e);
      return null;
    }
  }

  private void registerVersionListener() {
    VersionBridge localBridge = versionBridge;
    if (localBridge == null) {
      return;
    }
    localBridge
      .createVersionListener()
      .ifPresent(l -> getServer().getPluginManager().registerEvents(l, this));
  }

  /**
   * Attempts to register the {@code /doubledoors} command using Paper's
   * {@code BasicCommand} API via reflection. Returns {@code true} on success
   * (Paper server), {@code false} if the Bukkit fallback should be used.
   */
  private boolean registerPaperCommand() {
    try {
      Class<?> basicCmdClass = Class.forName(
        "io.papermc.paper.command.brigadier.BasicCommand"
      );
      Class<?> cssClass = Class.forName(
        "io.papermc.paper.command.brigadier.CommandSourceStack"
      );
      Class<?> senderClass = Class.forName(
        "org.bukkit.command.CommandSender"
      );

      java.lang.reflect.Method getSender = cssClass.getMethod("getSender");

      Object basicCommand = java.lang.reflect.Proxy.newProxyInstance(
        basicCmdClass.getClassLoader(),
        new Class<?>[]{ basicCmdClass },
        (proxy, method, args) -> {
          String name = method.getName();
          return switch (name) {
            case "execute" -> {
              Object source = args[0];
              String[] cmdArgs = (String[]) args[1];
              CommandSender sender = (CommandSender) getSender.invoke(source);
              handleCommand(sender, "doubledoors", cmdArgs);
              yield null;
            }
            case "suggest" -> {
              Object source = args[0];
              String[] cmdArgs = (String[]) args[1];
              CommandSender sender = (CommandSender) getSender.invoke(source);
              yield handleTabComplete(sender, "doubledoors", cmdArgs);
            }
            case "permission" -> null;
            case "canUse" -> true;
            default -> {
              try {
                yield method.invoke(proxy, args);
              } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
              }
            }
          };
        }
      );

      Class<?> javaPluginClass = Class.forName(
        "org.bukkit.plugin.java.JavaPlugin"
      );
      java.lang.reflect.Method registerCmd = javaPluginClass.getMethod(
        "registerCommand",
        String.class,
        java.util.Collection.class,
        basicCmdClass
      );
      registerCmd.invoke(
        this,
        "doubledoors",
        java.util.List.of("dd"),
        basicCommand
      );
      return true;
    } catch (Throwable ignored) {
      return false;
    }
  }

  private String t(String key, Object... args) {
    if (translationManager == null) {
      return key;
    }
    return translationManager.tr(key, args);
  }

  private String t(Player player, String key, Object... args) {
    if (translationManager == null) {
      return key;
    }
    return translationManager.tr(player, key, args);
  }

  /**
   * Loads a language file from plugin resources or data folder.
   * Called by TranslationManager via method reference.
   *
   * @param languageCode language code
   * @return translations map
   */
  public Map<String, String> loadLanguageFile(String languageCode) {
    return TranslationCatalog.loadLanguageFile(this, languageCode);
  }

  private static boolean hasAnyPluginEnabled(
    PluginManager pluginManager,
    String... pluginNames
  ) {
    for (Plugin plugin : pluginManager.getPlugins()) {
      if (!plugin.isEnabled()) {
        continue;
      }
      String installedName = plugin.getName();
      for (String candidate : pluginNames) {
        if (installedName.equalsIgnoreCase(candidate)) {
          return true;
        }
      }
    }
    return false;
  }

  private void initializeSqlIfEnabledAsync() {
    final int capturedGeneration = ++initGeneration;
    sqlStorage = null;
    if (!pluginConfig.isSqlEnabled()) {
      return;
    }

    SharedSqlStorage storage = new BukkitSharedSqlStorage(this, pluginConfig);
    SchedulerBridge.runAsync(this, () -> {
      try {
        storage.initializeSchema();
        if (pluginConfig.isMigrateYamlToSql()) {
          YamlToSqlMigrator.migrateIfNeeded(this, storage);
        }
        boolean proxyGeyserBridgeAvailable = storage.hasRecentProxyGeyserBridge(
          pluginConfig.getProxyHeartbeatMaxAgeMillis()
        );
        SchedulerBridge.runNextTick(this, () -> {
          if (capturedGeneration != initGeneration || !isEnabled()) {
            return;
          }
          closePlayerPreferences();
          sqlStorage = storage;
          playerPreferences = new PlayerPreferences(this);
          claimSettings = new ClaimSettings(this);
          setGeyserBridgeAvailable(
            localGeyserBridgeAvailable || proxyGeyserBridgeAvailable
          );
          restartProxyBridgePolling();
        });
      } catch (RuntimeException e) {
        getLogger().log(
          Level.SEVERE,
          "Could not initialize SQL storage; continuing with YAML persistence.",
          e
        );
        SchedulerBridge.runNextTick(this, () -> {
          if (capturedGeneration != initGeneration || !isEnabled()) {
            return;
          }
          closePlayerPreferences();
          sqlStorage = null;
          playerPreferences = new PlayerPreferences(this);
          claimSettings = new ClaimSettings(this);
          setGeyserBridgeAvailable(localGeyserBridgeAvailable);
          stopProxyBridgePolling();
        });
      }
    });
  }

  private void restartProxyBridgePolling() {
    stopProxyBridgePolling();
    SharedSqlStorage storage = sqlStorage;
    if (storage == null) {
      return;
    }

    long pollPeriodTicks = Math.max(
      1L,
      pluginConfig.getProxyHeartbeatMaxAgeMillis() / 50L
    );
    proxyBridgePollTask = SchedulerBridge.runTimerAsync(
      this,
      20L,
      pollPeriodTicks,
      () -> {
        SharedSqlStorage currentStorage = sqlStorage;
        if (currentStorage == null) {
          setGeyserBridgeAvailable(localGeyserBridgeAvailable);
          return;
        }

        boolean proxyBridgeAvailable =
          currentStorage.hasRecentProxyGeyserBridge(
            pluginConfig.getProxyHeartbeatMaxAgeMillis()
          );
        setGeyserBridgeAvailable(
          localGeyserBridgeAvailable || proxyBridgeAvailable
        );
      }
    );
  }

  private void stopProxyBridgePolling() {
    TaskToken localTask = proxyBridgePollTask;
    proxyBridgePollTask = null;
    if (localTask != null) {
      localTask.cancel();
    }
  }

  private void setGeyserBridgeAvailable(boolean available) {
    geyserBridgeAvailable = available;
    if (available && !geyserBridgeLogged) {
      geyserBridgeLogged = true;
      getLogger().info(t("log.geyser_detected"));
    }
  }

  private void closePlayerPreferences() {
    PlayerPreferences preferences = playerPreferences;
    if (preferences != null) {
      preferences.close();
      playerPreferences = null;
    }
  }

  @Override
  public boolean onCommand(
    CommandSender sender,
    Command command,
    String label,
    String[] args
  ) {
    return handleCommand(sender, label, args);
  }

  /**
   * Handles the {@code /doubledoors} command tree.
   *
   * @param sender the command sender
   * @param label the alias used to execute the command
   * @param args command arguments
   * @return {@code true} when the command was handled by this plugin
   */
  public boolean handleCommand(
    CommandSender sender,
    String label,
    String[] args
  ) {
    java.util.Objects.requireNonNull(sender, "sender");
    java.util.Objects.requireNonNull(label, "label");
    java.util.Objects.requireNonNull(args, "args");

    if (args.length == 0) {
      sender.sendMessage(mainUsage(label));
      return true;
    }

    if (args[0].equalsIgnoreCase("reload")) {
      if (!sender.hasPermission("doubledoors.reload")) {
        sender.sendMessage(t("cmd.no_permission"));
        return true;
      }

      reloadConfig();
      closePlayerPreferences();
      pluginConfig.reload();
      DoorUtil.setMirrorCacheTtlMillis(pluginConfig.getLookupCacheTtlMillis());
      fastStats.restart(pluginConfig, geyserBridgeAvailable);
      pluginUpdater.initialize(pluginConfig);
      stopProxyBridgePolling();
      sqlStorage = null;
      localGeyserBridgeAvailable = hasAnyPluginEnabled(
        getServer().getPluginManager(),
        "Geyser-Spigot",
        "Geyser",
        "floodgate",
        "floodgate-bukkit"
      );
      setGeyserBridgeAvailable(localGeyserBridgeAvailable);
      playerPreferences = new PlayerPreferences(this);
      claimSettings = new ClaimSettings(this);
      initializeSqlIfEnabledAsync();
      translationManager.reload();
      sender.sendMessage(
        t("cmd.reload.success", translationManager.getActiveLanguage())
      );
      return true;
    }

    if (args[0].equalsIgnoreCase("toggle")) {
      if (!(sender instanceof Player player)) {
        sender.sendMessage(t("cmd.only_players.toggle", label));
        return true;
      }

      // /doubledoors toggle [doors|gates|trapdoors|autoclose|knock]
      if (args.length >= 2) {
        if (!sender.hasPermission("doubledoors.toggle")) {
          sender.sendMessage(t(player, "cmd.no_permission"));
          return true;
        }

        UUID uuid = player.getUniqueId();
        switch (args[1].toLowerCase()) {
          case "doors" -> {
            boolean next = playerPreferences.toggleDoors(uuid);
            sender.sendMessage(
              next
                ? t(player, "cmd.toggle.doors.enabled")
                : t(player, "cmd.toggle.doors.disabled")
            );
          }
          case "gates" -> {
            boolean next = playerPreferences.toggleFenceGates(uuid);
            sender.sendMessage(
              next
                ? t(player, "cmd.toggle.gates.enabled")
                : t(player, "cmd.toggle.gates.disabled")
            );
          }
          case "trapdoors" -> {
            boolean next = playerPreferences.toggleTrapdoors(uuid);
            sender.sendMessage(
              next
                ? t(player, "cmd.toggle.trapdoors.enabled")
                : t(player, "cmd.toggle.trapdoors.disabled")
            );
          }
          case "autoclose" -> {
            if (!sender.hasPermission("doubledoors.toggle.autoclose")) {
              sender.sendMessage(t(player, "cmd.no_permission"));
              return true;
            }
            boolean next = playerPreferences.toggleAutoClose(uuid);
            sender.sendMessage(
              next
                ? t(player, "cmd.toggle.autoclose.enabled")
                : t(player, "cmd.toggle.autoclose.disabled")
            );
          }
          case "knock" -> {
            if (!sender.hasPermission("doubledoors.toggle.knock")) {
              sender.sendMessage(t(player, "cmd.no_permission"));
              return true;
            }
            boolean next = playerPreferences.toggleKnockSound(uuid);
            sender.sendMessage(
              next
                ? t(player, "cmd.toggle.knock.enabled")
                : t(player, "cmd.toggle.knock.disabled")
            );
          }
          default -> sender.sendMessage(t(player, "cmd.usage.toggle", label));
        }
        return true;
      }

      if (!sender.hasPermission("doubledoors.toggle")) {
        sender.sendMessage(t(player, "cmd.no_permission"));
        return true;
      }
      boolean enabled = playerPreferences.toggleAll(player.getUniqueId());
      sender.sendMessage(
        enabled
          ? t(player, "cmd.toggle.all.enabled")
          : t(player, "cmd.toggle.all.disabled")
      );
      return true;
    }

    if (args[0].equalsIgnoreCase("locale")) {
      if (!(sender instanceof Player player)) {
        sender.sendMessage(t("cmd.only_players.locale", label));
        return true;
      }
      if (!sender.hasPermission(LOCALE_PERMISSION)) {
        sender.sendMessage(t(player, "cmd.no_permission"));
        return true;
      }
      if (!pluginConfig.isPerPlayerLocaleEnabled()) {
        sender.sendMessage(t("cmd.locale.disabled"));
        return true;
      }
      if (args.length == 1) {
        sendLocaleStatus(player);
        return true;
      }
      String requested = args[1];
      if (requested.equalsIgnoreCase("credits")) {
        sendLocaleCredits(player);
        return true;
      }
      if (requested.equalsIgnoreCase("credit")) {
        if (args.length < 3) {
          sender.sendMessage(t(player, "cmd.usage.locale.credit", label));
          return true;
        }
        sendLocaleCredit(player, args[2]);
        return true;
      }
      String normalized = playerPreferences.setLocale(
        player.getUniqueId(),
        requested
      );
      if (normalized.isBlank()) {
        sender.sendMessage(t(player, "cmd.locale.cleared"));
      } else {
        sender.sendMessage(
          t(
            player,
            "cmd.locale.set",
            translationManager.getLanguageName(normalized)
          )
        );
      }
      return true;
    }

    if (args[0].equalsIgnoreCase("knock-volume")) {
      if (!(sender instanceof Player player)) {
        sender.sendMessage(t("cmd.only_players.knock_volume", label));
        return true;
      }
      if (!sender.hasPermission("doubledoors.knock.volume")) {
        sender.sendMessage(t(player, "cmd.no_permission"));
        return true;
      }
      if (args.length < 2) {
        sender.sendMessage(t(player, "cmd.usage.knock_volume", label));
        return true;
      }

      double volume;
      try {
        volume = Double.parseDouble(args[1]);
      } catch (NumberFormatException ex) {
        sender.sendMessage(t(player, "cmd.knock_volume.invalid", args[1]));
        return true;
      }

      if (!Double.isFinite(volume)) {
        sender.sendMessage(t(player, "cmd.knock_volume.invalid", args[1]));
        return true;
      }

      if (volume < 0.0 || volume > 1.0) {
        sender.sendMessage(t(player, "cmd.knock_volume.invalid", args[1]));
        return true;
      }

      double normalized = playerPreferences.setKnockVolume(
        player.getUniqueId(),
        volume
      );
      sender.sendMessage(t(player, "cmd.knock_volume.set", normalized));
      return true;
    }

    if (args[0].equalsIgnoreCase("server-toggle")) {
      if (!sender.hasPermission("doubledoors.server-toggle")) {
        sender.sendMessage(t("cmd.no_permission"));
        return true;
      }

      boolean nextState = !pluginConfig.isServerWideEnabled();
      pluginConfig.setServerWideEnabled(nextState);
      sender.sendMessage(
        nextState
          ? t("cmd.server_toggle.enabled")
          : t("cmd.server_toggle.disabled")
      );
      return true;
    }

    if (args[0].equalsIgnoreCase("debug")) {
      if (!(sender instanceof Player player)) {
        sender.sendMessage(t("cmd.only_players.debug", label));
        return true;
      }
      if (!sender.hasPermission("doubledoors.debug")) {
        sender.sendMessage(t(player, "cmd.no_permission"));
        return true;
      }

      boolean enabled = toggleDebug(player);
      sender.sendMessage(
        enabled
          ? t(player, "cmd.debug.enabled")
          : t(player, "cmd.debug.disabled")
      );
      return true;
    }

    if (args[0].equalsIgnoreCase("preview")) {
      if (!(sender instanceof Player player)) {
        sender.sendMessage(t("cmd.only_players.preview", label));
        return true;
      }
      if (!sender.hasPermission("doubledoors.preview")) {
        sender.sendMessage(t(player, "cmd.no_permission"));
        return true;
      }

      showPreview(player);
      return true;
    }

    if (args[0].equalsIgnoreCase("grief")) {
      if (!(sender instanceof Player player)) {
        sender.sendMessage(t("cmd.only_players.grief", label));
        return true;
      }

      if (!sender.hasPermission("doubledoors.grief")) {
        sender.sendMessage(t(player, "cmd.no_permission"));
        return true;
      }

      if (args.length < 2 || !args[1].equalsIgnoreCase("villagers")) {
        sender.sendMessage(t(player, "cmd.usage.grief", label));
        return true;
      }

      var playerLocation = java.util.Objects.requireNonNull(
        player.getLocation(),
        "player location"
      );
      Block standingBlock = playerLocation.getBlock();
      long claimId = ProtectionCompat.getClaimIdAt(this, standingBlock);
      if (claimId < 0) {
        sender.sendMessage(t(player, "cmd.grief.no_claim"));
        return true;
      }

      if (!ProtectionCompat.isClaimManagerAt(this, player, standingBlock)) {
        sender.sendMessage(t(player, "cmd.grief.no_manage_permission"));
        return true;
      }

      boolean blocked = claimSettings.toggleVillagersBlocked(claimId);
      sender.sendMessage(
        blocked
          ? t(player, "cmd.grief.villagers.blocked")
          : t(player, "cmd.grief.villagers.allowed")
      );
      return true;
    }

    sender.sendMessage(mainUsage(label));
    return true;
  }

  private String mainUsage(String label) {
    String version = getPluginMeta().getVersion();
    return String.join(
      "\n",
      "§6§l DoubleDoors §7v" + version,
      "§7-----------------------------------",
      usageLine(label, "reload", t("cmd.usage.main.reload")),
      usageLine(label, "toggle", t("cmd.usage.main.toggle")),
      usageLine(label, "knock-volume <0-1>", t("cmd.usage.main.knock_volume")),
      usageLine(label, "server-toggle", t("cmd.usage.main.server_toggle")),
      usageLine(label, "locale [code|credits|credit <code>]", t("cmd.usage.main.locale")),
      usageLine(label, "grief villagers", t("cmd.usage.main.grief")),
      usageLine(label, "debug", t("cmd.usage.main.debug")),
      usageLine(label, "preview", t("cmd.usage.main.preview")),
      "§7-----------------------------------"
    );
  }

  private String usageLine(String label, String syntax, String description) {
    return " §e/" + label + " " + syntax + " §7" + description;
  }

  @Override
  public List<String> onTabComplete(
    CommandSender sender,
    Command command,
    String alias,
    String[] args
  ) {
    return handleTabComplete(sender, alias, args);
  }

  /**
   * Provides tab completions for the {@code /doubledoors} command tree.
   *
   * @param sender the command sender
   * @param alias the alias used for completion
   * @param args current command arguments
   * @return matching completion candidates
   */
  public List<String> handleTabComplete(
    CommandSender sender,
    String alias,
    String[] args
  ) {
    java.util.Objects.requireNonNull(sender, "sender");
    java.util.Objects.requireNonNull(alias, "alias");
    java.util.Objects.requireNonNull(args, "args");
    List<String> completions = new ArrayList<>();

    if (args.length == 1) {
      for (String sub : List.of(
        "reload",
        "toggle",
        "knock-volume",
        "locale",
        "server-toggle",
        "grief",
        "debug",
        "preview"
      )) {
        if (sub.startsWith(args[0].toLowerCase())) {
          completions.add(sub);
        }
      }
    } else if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) {
      for (String sub : List.of(
        "doors",
        "gates",
        "trapdoors",
        "autoclose",
        "knock"
      )) {
        if (sub.startsWith(args[1].toLowerCase())) {
          completions.add(sub);
        }
      }
    } else if (args.length == 2 && args[0].equalsIgnoreCase("knock-volume")) {
      if (!args[1].isBlank() && "0.5".startsWith(args[1].toLowerCase())) {
        completions.add("0.5");
      }
    } else if (args.length == 2 && args[0].equalsIgnoreCase("locale")) {
      for (String sub : List.of("credits", "credit")) {
        if (sub.startsWith(args[1].toLowerCase())) {
          completions.add(sub);
        }
      }
      for (String languageCode : translationManager.getAvailableLanguages()) {
        if (languageCode.toLowerCase().startsWith(args[1].toLowerCase())) {
          completions.add(languageCode);
        }
      }
    } else if (
      args.length == 3 &&
      args[0].equalsIgnoreCase("locale") &&
      args[1].equalsIgnoreCase("credit")
    ) {
      for (String languageCode : translationManager.getAvailableLanguages()) {
        if (languageCode.toLowerCase().startsWith(args[2].toLowerCase())) {
          completions.add(languageCode);
        }
      }
    } else if (args.length == 2 && args[0].equalsIgnoreCase("grief")) {
      if ("villagers".startsWith(args[1].toLowerCase())) {
        completions.add("villagers");
      }
    }
    return completions;
  }

  private void sendLocaleStatus(Player player) {
    String current = playerPreferences.getLocale(player.getUniqueId());
    if (current.isBlank()) {
      String defaultCode = pluginConfig.getLanguage();
      player.sendMessage(
        t(
          player,
          "cmd.locale.current_default",
          translationManager.getLanguageName(defaultCode)
        )
      );
    } else {
      player.sendMessage(
        t(
          player,
          "cmd.locale.current",
          translationManager.getLanguageName(current)
        )
      );
    }
    player.sendMessage(t(player, "cmd.locale.available"));
    translationManager
      .getAvailableLanguages()
      .stream()
      .sorted(String::compareToIgnoreCase)
      .forEach(code -> {
        double pct = translationManager.getCompletionPercentage(code);
        player.sendMessage(
          t(
            player,
            "cmd.locale.available_entry",
            translationManager.getLanguageName(code),
            code,
            pct
          )
        );
      });
  }

  private void sendLocaleCredits(Player player) {
    player.sendMessage(t(player, "cmd.locale.credits.title"));
    List<String> languageCodes = new ArrayList<>(
      translationManager.getAvailableLanguages()
    );
    languageCodes.sort(String::compareToIgnoreCase);
    for (String languageCode : languageCodes) {
      List<String> credits = translationManager.getLanguageCredits(
        languageCode
      );
      if (credits.isEmpty()) {
        player.sendMessage(t(player, "cmd.locale.credit.none", languageCode));
        continue;
      }
      player.sendMessage(
        t(
          player,
          "cmd.locale.credit.entry",
          translationManager.getLanguageName(languageCode),
          languageCode,
          String.join(", ", credits)
        )
      );
    }
  }

  private void sendLocaleCredit(Player player, String languageCode) {
    String canonicalLanguageCode = translationManager
      .getAvailableLanguages()
      .stream()
      .filter(code -> code.equalsIgnoreCase(languageCode))
      .findFirst()
      .orElse(null);
    if (canonicalLanguageCode == null) {
      player.sendMessage(t(player, "cmd.locale.credit.unknown", languageCode));
      return;
    }

    List<String> credits = translationManager.getLanguageCredits(
      canonicalLanguageCode
    );
    if (credits.isEmpty()) {
      player.sendMessage(
        t(player, "cmd.locale.credit.none", canonicalLanguageCode)
      );
      return;
    }
    player.sendMessage(
      t(
        player,
        "cmd.locale.credit.entry",
        translationManager.getLanguageName(canonicalLanguageCode),
        canonicalLanguageCode,
        String.join(", ", credits)
      )
    );
  }

  private void showPreview(Player player) {
    Block origin = player.getTargetBlockExact(8);
    if (origin == null) {
      player.sendMessage(t(player, "cmd.preview.no_target"));
      return;
    }
    if (
      !DoorInteractListener.isEnabledTypeForDebug(
        origin.getType(),
        pluginConfig,
        this
      )
    ) {
      player.sendMessage(
        t(player, "cmd.preview.unsupported", origin.getType().name())
      );
      return;
    }

    Block partner;
    String facing;
    if (origin.getBlockData() instanceof Door) {
      DoorUtil.MirrorSearchResult result =
        DoorUtil.analyzeMirroredDoubleDoorPartner(origin);
      if (!result.found()) {
        result = DoorUtil.analyzeCornerDoorPartner(origin);
      }
      if (!result.found()) {
        player.sendMessage(t(player, "cmd.preview.not_found", result.reason()));
        return;
      }
      partner = result.partner();
      facing = ((Door) partner.getBlockData()).getFacing().name();
    } else {
      var connected = DoorUtil.findConnectedDoors(
        origin,
        pluginConfig.getRecursiveOpeningMaxBlocksDistance()
      );
      if (connected.isEmpty()) {
        player.sendMessage(
          t(player, "cmd.preview.not_found", "no_connected_block")
        );
        return;
      }
      partner = connected.iterator().next();
      facing = "N/A";
    }

    Location center = partner.getLocation().add(0.5, 0.5, 0.5);
    player.sendMessage(
      t(
        player,
        "cmd.preview.found",
        partner.getWorld().getName(),
        partner.getX(),
        partner.getY(),
        partner.getZ(),
        facing
      )
    );

    int bursts = Math.max(1, pluginConfig.getPreviewDurationTicks() / 10);
    for (int i = 0; i < bursts; i++) {
      int tickDelay = Math.max(1, i * 10);
      SchedulerBridge.runLaterAtLocation(this, center, tickDelay, () ->
        partner
          .getWorld()
          .spawnParticle(
            pluginConfig.getPreviewParticle(),
            center,
            pluginConfig.getPreviewParticleCount(),
            0.22,
            0.30,
            0.22,
            0.01
          )
      );
    }
  }
}
