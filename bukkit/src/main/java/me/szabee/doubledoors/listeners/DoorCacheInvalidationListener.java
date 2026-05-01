package me.szabee.doubledoors.listeners;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockExplodeEvent;

import me.szabee.doubledoors.util.DoorUtil;
import me.szabee.doubledoors.util.OpenableType;

/**
 * Invalidates cached mirror lookups when door blocks change.
 */
public final class DoorCacheInvalidationListener implements Listener {
  /**
   * Invalidates cache entries on door placement.
   *
   * @param event the place event
   */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    invalidateIfDoor(event.getBlockPlaced());
  }

  /**
   * Invalidates cache entries on door break.
   *
   * @param event the break event
   */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    invalidateIfDoor(event.getBlock());
  }

  /**
   * Invalidates cache entries on entity explosions.
   *
   * @param event the explode event
   */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onEntityExplode(EntityExplodeEvent event) {
    for (Block block : event.blockList()) {
      invalidateIfDoor(block);
    }
  }

  /**
   * Invalidates cache entries on block explosions.
   *
   * @param event the explode event
   */
  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onBlockExplode(BlockExplodeEvent event) {
    for (Block block : event.blockList()) {
      invalidateIfDoor(block);
    }
  }

  private void invalidateIfDoor(Block block) {
    if (block == null) {
      return;
    }
    if (OpenableType.fromBlockData(block.getBlockData(), block.getType()) != OpenableType.DOOR) {
      return;
    }
    DoorUtil.invalidateMirrorCacheNear(block);
  }
}
