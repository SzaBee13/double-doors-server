package me.szabee.doubledoors;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

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

import dev.faststats.bukkit.BukkitMetrics;
import dev.faststats.core.data.Metric;
import me.szabee.doubledoors.api.DoubleDoorsAPI;
import me.szabee.doubledoors.config.ClaimSettings;
import me.szabee.doubledoors.config.PlayerPreferences;
import me.szabee.doubledoors.config.PluginConfig;
import me.szabee.doubledoors.i18n.TranslationManager;
import me.szabee.doubledoors.listeners.DoorInteractListener;
import me.szabee.doubledoors.listeners.RedstoneListener;
import me.szabee.doubledoors.migration.YamlToSqlMigrator;
import me.szabee.doubledoors.storage.SharedSqlStorage;
import me.szabee.doubledoors.util.DoorUtil;
import me.szabee.doubledoors.util.OpenableType;
import me.szabee.doubledoors.util.ProtectionCompat;
import me.szabee.doubledoors.util.SchedulerBridge;
import org.lushplugins.pluginupdater.api.updater.Updater;

/**
 * Main plugin class for DoubleDoors.
 */
public final class DoubleDoors extends JavaPlugin {
  private static final String FASTSTATS_TOKEN_PATTERN = "[a-z0-9]{32}";
  private static final String FASTSTATS_PROJECT_TOKEN = "883c734d766f7078fa4525e9c573c8af"; // This should be public since it only identifies the project, not individual servers.
  private static final String MODRINTH_PROJECT_ID = "Fdj5mcgC";
  private static final String UPDATE_NOTIFY_PERMISSION = "doubledoors.update.notify";

  private volatile PluginConfig pluginConfig;
  private volatile PlayerPreferences playerPreferences;
  private volatile ClaimSettings claimSettings;
  private volatile TranslationManager translationManager;
  private volatile SharedSqlStorage sqlStorage;
  private volatile BukkitMetrics metrics;
  private volatile Updater updater;
  private final Set<UUID> debugPlayers = ConcurrentHashMap.newKeySet();
  private final Set<Material> customOpenables = ConcurrentHashMap.newKeySet();
  private final DoubleDoorsAPI api = new DoubleDoorsAPI() {
    @Override
    public boolean isDoubleBehaviorEnabled(Player player) {
      return player != null && isEnabledForPlayer(player);
    }

    @Override
    public boolean triggerLinkedOpen(Block origin, Player actor) {
      if (origin == null) {
        return false;
      }

      BlockData data = origin.getBlockData();
      if (!(data instanceof Openable openable)) {
        return false;
      }

      boolean targetState = !openable.isOpen();
      if (data instanceof Door) {
        DoorUtil.MirrorSearchResult search = DoorUtil.analyzeMirroredDoubleDoorPartner(origin);
        if (!search.found()) {
          return false;
        }
        Block partner = search.partner();
        if (actor != null) {
          String denyReason = ProtectionCompat.explainLinkedDoorDeniedReason(DoubleDoors.this, actor, partner);
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

      var connected = DoorUtil.findConnectedDoors(origin, pluginConfig.getRecursiveOpeningMaxBlocksDistance());
      if (connected.isEmpty()) {
        return false;
      }

      boolean changedAny = false;
      for (Block block : connected) {
        if (actor != null) {
          String denyReason = ProtectionCompat.explainLinkedDoorDeniedReason(DoubleDoors.this, actor, block);
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

    @Override
    public void registerCustomOpenableBlock(Material material) {
      if (material != null) {
        customOpenables.add(material);
      }
    }

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
   * Gets the public DoubleDoors API for other plugins.
   *
   * @return the API surface
   */
  public DoubleDoorsAPI getApi() {
    return api;
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
  public String explainLinkedDoorDeniedReason(Player player, Block linkedBlock) {
    return ProtectionCompat.explainLinkedDoorDeniedReason(this, player, linkedBlock);
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
      linkedBlock.getWorld().spawnParticle(
          pluginConfig.getPartnerParticle(),
          loc,
          pluginConfig.getPartnerParticleCount(),
          0.18,
          0.20,
          0.18,
          0.01);
    }
  }

  @Override
  public void onEnable() {
    saveDefaultConfig();
    pluginConfig = new PluginConfig(this);
    DoorUtil.setMirrorCacheTtlMillis(pluginConfig.getLookupCacheTtlMillis());

    restartFastStats();
    initializeUpdater();

    sqlStorage = null;
    translationManager = new TranslationManager(this, pluginConfig);
    translationManager.reload();
    playerPreferences = new PlayerPreferences(this);
    claimSettings = new ClaimSettings(this);
    initializeSqlIfEnabledAsync();

    getServer().getPluginManager().registerEvents(new DoorInteractListener(this), this);
    getServer().getPluginManager().registerEvents(new RedstoneListener(this), this);

    var doubledoorsCommand = getCommand("doubledoors");
    if (doubledoorsCommand != null) {
      doubledoorsCommand.setExecutor(this);
      doubledoorsCommand.setTabCompleter(this);
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
    boolean hasLocalGeyserBridge = hasAnyPluginEnabled(pluginManager,
        "Geyser-Spigot",
        "Geyser",
        "floodgate",
        "floodgate-bukkit");
    boolean hasProxyHeartbeat = sqlStorage != null
      && sqlStorage.hasRecentProxyHeartbeat(pluginConfig.getProxyHeartbeatMaxAgeMillis());
    if (hasLocalGeyserBridge || hasProxyHeartbeat) {
      getLogger().info(t("log.geyser_detected"));
    }

    getLogger().info(t("log.enabled"));
  }

  @Override
  public void onDisable() {
    try {
      if (metrics != null) {
        try {
          metrics.shutdown();
        } catch (RuntimeException e) {
          getLogger().log(Level.WARNING, "FastStats could not be shut down cleanly.", e);
        } finally {
          metrics = null;
        }
      }
    } finally {
      if (playerPreferences != null) {
        playerPreferences.save();
      }
      getLogger().info(t("log.disabled"));
    }
  }

  private String t(String key, Object... args) {
    if (translationManager == null) {
      return key;
    }
    return translationManager.tr(key, args);
  }

  private static boolean hasAnyPluginEnabled(PluginManager pluginManager, String... pluginNames) {
    for (Plugin plugin : pluginManager.getPlugins()) {
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
    sqlStorage = null;
    if (!pluginConfig.isSqlEnabled()) {
      return;
    }

    SharedSqlStorage storage = new SharedSqlStorage(this, pluginConfig);
    SchedulerBridge.runAsync(this, () -> {
      try {
        storage.initializeSchema();
        if (pluginConfig.isMigrateYamlToSql()) {
          YamlToSqlMigrator.migrateIfNeeded(this, storage);
        }
        SchedulerBridge.runNextTick(this, () -> {
          sqlStorage = storage;
          playerPreferences = new PlayerPreferences(this);
          claimSettings = new ClaimSettings(this);
        });
      } catch (RuntimeException e) {
        getLogger().log(Level.SEVERE, "Could not initialize SQL storage; continuing with YAML persistence.", e);
        SchedulerBridge.runNextTick(this, () -> {
          sqlStorage = null;
          playerPreferences = new PlayerPreferences(this);
          claimSettings = new ClaimSettings(this);
        });
      }
    });
  }

  private void initializeFastStats() {
    if (!pluginConfig.isEnableAnonymousTracking()) {
      metrics = null;
      getLogger().info("Anonymous tracking is disabled by config.");
      return;
    }

    String token = normalizeFastStatsToken(FASTSTATS_PROJECT_TOKEN);
    if (token == null) {
      metrics = null;
      getLogger().warning("Anonymous tracking is enabled, but the built-in FastStats token is invalid; metrics are disabled.");
      return;
    }

    BukkitMetrics.Factory factory = BukkitMetrics.factory();
    if (pluginConfig.isEnableExtendedAnonymousTracking()) {
      factory = factory
          .addMetric(Metric.string("server_location", pluginConfig::getTrackingServerLocation))
          .addMetric(Metric.stringArray("countries", () -> pluginConfig.getTrackingCountries().toArray(String[]::new)))
          .addMetric(Metric.string("java_version", () -> System.getProperty("java.version", "unknown")))
          .addMetric(Metric.stringArray("system_statistics", this::getSystemStatistics));
    }

    try {
      BukkitMetrics localMetrics = factory.token(token).create(this);
      localMetrics.ready();
      metrics = localMetrics;
    } catch (RuntimeException e) {
      metrics = null;
      getLogger().log(Level.WARNING, "FastStats could not be initialized; continuing without metrics.", e);
    }
  }

  private void restartFastStats() {
    if (metrics != null) {
      try {
        metrics.shutdown();
      } catch (RuntimeException e) {
        getLogger().log(Level.WARNING, "FastStats could not be shut down cleanly during restart.", e);
      } finally {
        metrics = null;
      }
    }
    initializeFastStats();
  }

  private void initializeUpdater() {
    updater = null;
    if (!pluginConfig.isUpdateCheckerEnabled()) {
      return;
    }

    try {
      updater = Updater.builder(this)
          .modrinth(MODRINTH_PROJECT_ID)
          .checkSchedule(pluginConfig.getUpdateCheckerScheduleSeconds())
          .notify(pluginConfig.isUpdateCheckerNotify())
          .notificationPermission(UPDATE_NOTIFY_PERMISSION)
          .build();
    } catch (RuntimeException | LinkageError e) {
      getLogger().log(Level.WARNING, "Plugin updater could not be initialized; continuing without update checks.", e);
    }
  }

  private String normalizeFastStatsToken(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      return null;
    }

    String normalized = rawToken.trim().toLowerCase().replace("-", "");
    if (!normalized.matches(FASTSTATS_TOKEN_PATTERN)) {
      return null;
    }
    return normalized;
  }

  private String[] getSystemStatistics() {
    Runtime runtime = Runtime.getRuntime();
    return new String[] {
        "os=" + System.getProperty("os.name", "unknown"),
        "os_version=" + System.getProperty("os.version", "unknown"),
        "arch=" + System.getProperty("os.arch", "unknown"),
        "java_version=" + System.getProperty("java.version", "unknown"),
        "cores=" + runtime.availableProcessors(),
        "max_memory_mb=" + (runtime.maxMemory() / (1024L * 1024L))
    };
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    java.util.Objects.requireNonNull(sender, "sender");
    java.util.Objects.requireNonNull(command, "command");
    java.util.Objects.requireNonNull(label, "label");
    java.util.Objects.requireNonNull(args, "args");
    if (!command.getName().equalsIgnoreCase("doubledoors")) {
      return false;
    }

    if (args.length == 0) {
      sender.sendMessage(t("cmd.usage.main", label));
      return true;
    }

    if (args[0].equalsIgnoreCase("reload")) {
      if (!sender.hasPermission("doubledoors.reload")) {
        sender.sendMessage(t("cmd.no_permission"));
        return true;
      }

      reloadConfig();
      pluginConfig.reload();
      DoorUtil.setMirrorCacheTtlMillis(pluginConfig.getLookupCacheTtlMillis());
      restartFastStats();
      sqlStorage = null;
      playerPreferences = new PlayerPreferences(this);
      claimSettings = new ClaimSettings(this);
      initializeSqlIfEnabledAsync();
      translationManager.reload();
      sender.sendMessage(t("cmd.reload.success", translationManager.getActiveLanguage()));
      return true;
    }

    if (args[0].equalsIgnoreCase("toggle")) {
      if (!(sender instanceof Player player)) {
        sender.sendMessage(t("cmd.only_players.toggle", label));
        return true;
      }

      if (!sender.hasPermission("doubledoors.toggle")) {
        sender.sendMessage(t("cmd.no_permission"));
        return true;
      }

      // /doubledoors toggle [doors|gates|trapdoors]
      if (args.length >= 2) {
        UUID uuid = player.getUniqueId();
        switch (args[1].toLowerCase()) {
          case "doors" -> {
            boolean next = playerPreferences.toggleDoors(uuid);
            sender.sendMessage(next ? t("cmd.toggle.doors.enabled") : t("cmd.toggle.doors.disabled"));
          }
          case "gates" -> {
            boolean next = playerPreferences.toggleFenceGates(uuid);
            sender.sendMessage(next ? t("cmd.toggle.gates.enabled") : t("cmd.toggle.gates.disabled"));
          }
          case "trapdoors" -> {
            boolean next = playerPreferences.toggleTrapdoors(uuid);
            sender.sendMessage(next ? t("cmd.toggle.trapdoors.enabled") : t("cmd.toggle.trapdoors.disabled"));
          }
          default -> sender.sendMessage(t("cmd.usage.toggle", label));
        }
        return true;
      }

      boolean enabled = playerPreferences.toggleAll(player.getUniqueId());
      sender.sendMessage(enabled ? t("cmd.toggle.all.enabled") : t("cmd.toggle.all.disabled"));
      return true;
    }

    if (args[0].equalsIgnoreCase("server-toggle")) {
      if (!sender.hasPermission("doubledoors.server-toggle")) {
        sender.sendMessage(t("cmd.no_permission"));
        return true;
      }

      boolean nextState = !pluginConfig.isServerWideEnabled();
      pluginConfig.setServerWideEnabled(nextState);
      sender.sendMessage(nextState
          ? t("cmd.server_toggle.enabled")
          : t("cmd.server_toggle.disabled"));
      return true;
    }

    if (args[0].equalsIgnoreCase("debug")) {
      if (!(sender instanceof Player player)) {
        sender.sendMessage(t("cmd.only_players.debug", label));
        return true;
      }
      if (!sender.hasPermission("doubledoors.debug")) {
        sender.sendMessage(t("cmd.no_permission"));
        return true;
      }

      boolean enabled = toggleDebug(player);
      sender.sendMessage(enabled ? t("cmd.debug.enabled") : t("cmd.debug.disabled"));
      return true;
    }

    if (args[0].equalsIgnoreCase("preview")) {
      if (!(sender instanceof Player player)) {
        sender.sendMessage(t("cmd.only_players.preview", label));
        return true;
      }
      if (!sender.hasPermission("doubledoors.preview")) {
        sender.sendMessage(t("cmd.no_permission"));
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
        sender.sendMessage(t("cmd.no_permission"));
        return true;
      }

      if (args.length < 2 || !args[1].equalsIgnoreCase("villagers")) {
        sender.sendMessage(t("cmd.usage.grief", label));
        return true;
      }

      var playerLocation = java.util.Objects.requireNonNull(player.getLocation(), "player location");
      Block standingBlock = playerLocation.getBlock();
      long claimId = ProtectionCompat.getClaimIdAt(this, standingBlock);
      if (claimId < 0) {
        sender.sendMessage(t("cmd.grief.no_claim"));
        return true;
      }

      if (!ProtectionCompat.isClaimManagerAt(this, player, standingBlock)) {
        sender.sendMessage(t("cmd.grief.no_manage_permission"));
        return true;
      }

      boolean blocked = claimSettings.toggleVillagersBlocked(claimId);
      sender.sendMessage(blocked
          ? t("cmd.grief.villagers.blocked")
          : t("cmd.grief.villagers.allowed"));
      return true;
    }

    sender.sendMessage(t("cmd.usage.main", label));
    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    java.util.Objects.requireNonNull(sender, "sender");
    java.util.Objects.requireNonNull(command, "command");
    java.util.Objects.requireNonNull(alias, "alias");
    java.util.Objects.requireNonNull(args, "args");
    List<String> completions = new ArrayList<>();
    if (!command.getName().equalsIgnoreCase("doubledoors")) {
      return completions;
    }

    if (args.length == 1) {
      for (String sub : List.of("reload", "toggle", "server-toggle", "grief", "debug", "preview")) {
        if (sub.startsWith(args[0].toLowerCase())) {
          completions.add(sub);
        }
      }
    } else if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) {
      for (String sub : List.of("doors", "gates", "trapdoors")) {
        if (sub.startsWith(args[1].toLowerCase())) {
          completions.add(sub);
        }
      }
    } else if (args.length == 2 && args[0].equalsIgnoreCase("grief")) {
      if ("villagers".startsWith(args[1].toLowerCase())) {
        completions.add("villagers");
      }
    }
    return completions;
  }

  private void showPreview(Player player) {
    Block origin = player.getTargetBlockExact(8);
    if (origin == null) {
      player.sendMessage(t("cmd.preview.no_target"));
      return;
    }
    if (!DoorInteractListener.isEnabledTypeForDebug(origin.getType(), pluginConfig, this)) {
      player.sendMessage(t("cmd.preview.unsupported", origin.getType().name()));
      return;
    }

    Block partner;
    String facing;
    if (origin.getBlockData() instanceof Door) {
      DoorUtil.MirrorSearchResult result = DoorUtil.analyzeMirroredDoubleDoorPartner(origin);
      if (!result.found()) {
        player.sendMessage(t("cmd.preview.not_found", result.reason()));
        return;
      }
      partner = result.partner();
      facing = ((Door) partner.getBlockData()).getFacing().name();
    } else {
      var connected = DoorUtil.findConnectedDoors(origin, pluginConfig.getRecursiveOpeningMaxBlocksDistance());
      if (connected.isEmpty()) {
        player.sendMessage(t("cmd.preview.not_found", "no_connected_block"));
        return;
      }
      partner = connected.iterator().next();
      facing = "N/A";
    }

    Location center = partner.getLocation().add(0.5, 0.5, 0.5);
    player.sendMessage(t(
        "cmd.preview.found",
        partner.getWorld().getName(),
        partner.getX(),
        partner.getY(),
        partner.getZ(),
        facing));

    int bursts = Math.max(1, pluginConfig.getPreviewDurationTicks() / 10);
    for (int i = 0; i < bursts; i++) {
      int tickDelay = Math.max(1, i * 10);
      SchedulerBridge.runLaterAtLocation(this, center, tickDelay, () -> partner.getWorld().spawnParticle(
          pluginConfig.getPreviewParticle(),
          center,
          pluginConfig.getPreviewParticleCount(),
          0.22,
          0.30,
          0.22,
          0.01));
    }
  }
}
