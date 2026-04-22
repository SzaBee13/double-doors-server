package me.szabee.doubledoors.listeners;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import me.szabee.doubledoors.DoubleDoors;
import me.szabee.doubledoors.config.PlayerPreferences;
import me.szabee.doubledoors.config.PluginConfig;
import me.szabee.doubledoors.util.DoorUtil;
import me.szabee.doubledoors.util.OpenableType;
import me.szabee.doubledoors.util.SchedulerBridge;

/**
 * Handles player interactions with doors, gates, and trapdoors.
 */
public final class DoorInteractListener implements Listener {
  private static final long DUPLICATE_INTERACTION_WINDOW_NANOS = 80_000_000L;

  private final DoubleDoors plugin;
  private final ConcurrentMap<UUID, InteractionStamp> lastInteractionByPlayer = new ConcurrentHashMap<>();

  /**
   * Creates a new interaction listener.
   *
   * @param plugin the plugin instance
   */
  public DoorInteractListener(DoubleDoors plugin) {
    this.plugin = plugin;
  }

  /**
   * Handles right-click block interactions for door-like blocks.
   *
   * @param event the interact event
   */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getHand() != EquipmentSlot.HAND) {
      return;
    }

    Block clicked = event.getClickedBlock();
    if (clicked == null) {
      return;
    }

    Player player = event.getPlayer();
    PluginConfig config = plugin.getPluginConfig();
    Action action = event.getAction();

    if (action == Action.LEFT_CLICK_BLOCK) {
      playDoorKnock(player, clicked, config);
      return;
    }
    if (action != Action.RIGHT_CLICK_BLOCK) {
      return;
    }

    if (!config.isServerWideEnabled()) {
      return;
    }

    if (!plugin.isEnabledForPlayer(player)) {
      return;
    }
    if (!player.hasPermission("doubledoors.use")) {
      return;
    }
    if (player.isSneaking()) {
      return;
    }
    if (!isEnabledTypeForPlayer(clicked.getType(), config, plugin.getPlayerPreferences(), player.getUniqueId())) {
      return;
    }
    if (!plugin.isLocationAllowed(clicked)) {
      if (plugin.isDebugEnabled(player)) {
        player.sendMessage(plugin.getTranslationManager().tr("cmd.debug.skip", "location_filter"));
      }
      return;
    }
    if (isDuplicateInteraction(player, clicked)) {
      return;
    }

    scheduleManualIronDoorToggleIfPermitted(player, clicked);
    applyConnectedState(player, clicked, config);
  }

  private void playDoorKnock(Player player, Block clicked, PluginConfig config) {
    if (!config.isEnableKnockFeature()) {
      return;
    }
    if (!player.hasPermission("doubledoors.knock")) {
      return;
    }
    if (!config.isEnableDoors()) {
      return;
    }
    if (!plugin.getPlayerPreferences().isDoorsEnabled(player.getUniqueId())) {
      return;
    }
    if (OpenableType.fromMaterial(clicked.getType()) != OpenableType.DOOR) {
      return;
    }

    Sound hitSound = clicked.getBlockData().getSoundGroup().getHitSound();
    if (hitSound == null) {
      return;
    }

    double maxDistance = config.getKnockDistanceBlocks();
    double maxDistanceSquared = maxDistance * maxDistance;
    var soundLocation = clicked.getLocation().add(0.5, 0.5, 0.5);
    for (Player nearby : clicked.getWorld().getPlayers()) {
      if (nearby.getLocation().distanceSquared(soundLocation) > maxDistanceSquared) {
        continue;
      }
      nearby.playSound(soundLocation, hitSound, SoundCategory.BLOCKS, 1.0f, 1.0f);
    }
  }

  /**
   * Cleans up debounce map entry when a player leaves to prevent memory leaks.
   *
   * @param event the player quit event
   */
  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    lastInteractionByPlayer.remove(event.getPlayer().getUniqueId());
  }

  private void applyConnectedState(Player player, Block origin, PluginConfig config) {
    if (!config.isEnableRecursiveOpening()) {
      return;
    }
    if (!(origin.getBlockData() instanceof Openable)) {
      return;
    }

    // Schedule for the next tick so we read the state AFTER vanilla has processed the
    // click. Reading it here (at MONITOR) would give the pre-click state on Paper 1.21,
    // causing the partner/connected blocks to be set to the wrong state.
    long delay = 1L + config.getAnimationSyncExtraDelayTicks();
    SchedulerBridge.runLaterAtLocation(plugin, origin.getLocation(), delay, () -> {
      BlockData originData = origin.getBlockData();
      if (!(originData instanceof Openable openable)) {
        return;
      }

      boolean openState = openable.isOpen();

      if (originData instanceof Door) {
        DoorUtil.MirrorSearchResult search = DoorUtil.analyzeMirroredDoubleDoorPartner(origin);
        if (!search.found()) {
          if (plugin.isDebugEnabled(player)) {
            player.sendMessage(plugin.getTranslationManager().tr("cmd.debug.partner_missing", search.reason()));
          }
          return;
        }
        Block partner = search.partner();
        if (!plugin.isLocationAllowed(partner)) {
          if (plugin.isDebugEnabled(player)) {
            player.sendMessage(plugin.getTranslationManager().tr("cmd.debug.partner_blocked", "location_filter"));
          }
          return;
        }

        BlockData partnerData = partner.getBlockData();
        if (!(partnerData instanceof Openable linked)) {
          if (plugin.isDebugEnabled(player)) {
            player.sendMessage(plugin.getTranslationManager().tr("cmd.debug.partner_blocked", "not_openable"));
          }
          return;
        }
        String denyReason = plugin.explainLinkedDoorDeniedReason(player, partner);
        if (!denyReason.isEmpty()) {
          if (plugin.isDebugEnabled(player)) {
            player.sendMessage(plugin.getTranslationManager().tr("cmd.debug.partner_blocked", denyReason));
          }
          return;
        }

        if (linked.isOpen() == openState) {
          return;
        }

        linked.setOpen(openState);
        partner.setBlockData(linked, false);
        plugin.playLinkedFeedback(partner, OpenableType.DOOR);

        // Doors are two blocks tall — update the upper half explicitly so both
        // halves stay in sync (setBlockData with applyPhysics=false does not
        // automatically propagate the open state to the adjacent half).
        Block partnerTop = partner.getRelative(BlockFace.UP);
        BlockData topData = partnerTop.getBlockData();
        if (topData instanceof Openable topOpenable) {
          topOpenable.setOpen(openState);
          partnerTop.setBlockData(topData, false);
        }
        return;
      }

      Set<Block> connected = DoorUtil.findConnectedDoors(origin, config.getRecursiveOpeningMaxBlocksDistance());
      if (connected.isEmpty()) {
        return;
      }

      for (Block block : connected) {
        BlockData data = block.getBlockData();
        if (!(data instanceof Openable linked)) {
          continue;
        }
        if (linked.isOpen() == openState) {
          continue;
        }

        if (!plugin.isLocationAllowed(block)) {
          continue;
        }

        linked.setOpen(openState);
        block.setBlockData(linked, false);
        OpenableType type = OpenableType.fromMaterial(block.getType());
        plugin.playLinkedFeedback(block, type == null ? OpenableType.CUSTOM : type);
      }
    });
  }

  private void scheduleManualIronDoorToggleIfPermitted(Player player, Block clicked) {
    if (!player.hasPermission("doubledoors.iron.manual")) {
      return;
    }
    if (clicked.getType() != Material.IRON_DOOR) {
      return;
    }

    Block baseDoor = toDoorBottomHalf(clicked);
    SchedulerBridge.runLaterAtLocation(plugin, baseDoor.getLocation(), 1L, () -> {
      BlockData data = baseDoor.getBlockData();
      if (!(data instanceof Openable openable)) {
        return;
      }
      openable.setOpen(!openable.isOpen());
      baseDoor.setBlockData(openable, false);

      Block top = baseDoor.getRelative(BlockFace.UP);
      BlockData topData = top.getBlockData();
      if (topData instanceof Openable topOpenable) {
        topOpenable.setOpen(openable.isOpen());
        top.setBlockData(topOpenable, false);
      }
    });
  }

  private boolean isEnabledTypeForPlayer(Material material, PluginConfig config, PlayerPreferences prefs, UUID playerId) {
    OpenableType type = OpenableType.fromMaterial(material);
    if (type == OpenableType.DOOR) {
      return config.isEnableDoors() && prefs.isDoorsEnabled(playerId);
    }
    if (type == OpenableType.FENCE_GATE) {
      return config.isEnableFenceGates() && prefs.isFenceGatesEnabled(playerId);
    }
    if (type == OpenableType.TRAPDOOR) {
      return config.isEnableTrapdoors() && prefs.isTrapdoorsEnabled(playerId);
    }
    return plugin.isCustomOpenable(material) && prefs.isEnabled(playerId);
  }  

  // Kept for code paths that do not involve a specific player (e.g. redstone / villager)
  static boolean isEnabledType(Material material, PluginConfig config) {
    OpenableType type = OpenableType.fromMaterial(material);
    if (type == OpenableType.DOOR) {
      return config.isEnableDoors();
    }
    if (type == OpenableType.FENCE_GATE) {
      return config.isEnableFenceGates();
    }
    if (type == OpenableType.TRAPDOOR) {
      return config.isEnableTrapdoors();
    }
    return false;
  }

  /**
   * Helper used by commands for type-checking against both built-ins and custom materials.
   *
   * @param material the material to evaluate
   * @param config active plugin config
   * @param plugin plugin instance
   * @return true when this material can be processed by linked behavior
   */
  public static boolean isEnabledTypeForDebug(Material material, PluginConfig config, DoubleDoors plugin) {
    return isEnabledType(material, config) || plugin.isCustomOpenable(material);
  }

  private boolean isDuplicateInteraction(Player player, Block clicked) {
    long now = System.nanoTime();
    UUID playerId = player.getUniqueId();
    InteractionStamp previous = lastInteractionByPlayer.get(playerId);

    InteractionStamp current = new InteractionStamp(
        clicked.getWorld().getUID(),
        clicked.getX(),
        clicked.getY(),
        clicked.getZ(),
        now
    );
    lastInteractionByPlayer.put(playerId, current);

    if (previous == null) {
      return false;
    }

    if (!previous.sameBlock(clicked)) {
      return false;
    }

    return (now - previous.timestampNanos()) <= DUPLICATE_INTERACTION_WINDOW_NANOS;
  }

  private Block toDoorBottomHalf(Block block) {
    if (!(block.getBlockData() instanceof Door doorData)) {
      return block;
    }
    if (!(doorData instanceof Bisected bisected) || bisected.getHalf() == Bisected.Half.BOTTOM) {
      return block;
    }
    return block.getRelative(BlockFace.DOWN);
  }

  private record InteractionStamp(UUID worldId, int x, int y, int z, long timestampNanos) {
    private boolean sameBlock(Block block) {
      return worldId.equals(block.getWorld().getUID())
          && x == block.getX()
          && y == block.getY()
          && z == block.getZ();
    }
  }
}
