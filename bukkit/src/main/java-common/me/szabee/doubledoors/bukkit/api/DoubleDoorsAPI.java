package me.szabee.doubledoors.bukkit.api;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Bukkit-facing DoubleDoors API.
 */
public interface DoubleDoorsAPI {
  /**
   * Returns whether linked-door behavior is enabled for the given player
   * (checks the player's personal preference).
   */
  boolean isDoubleBehaviorEnabled(Player player);

  /**
   * Programmatically triggers the linked open/close behavior for all doors
   * connected to the given origin block.
   *
   * @param origin the block to start the linked toggle from
   * @param actor  the player who triggered the action, or {@code null} for
   *               environmental triggers (redstone, villagers)
   * @return true if at least one linked block was toggled
   */
  boolean triggerLinkedOpen(Block origin, @Nullable Player actor);

  /**
   * Registers a custom material as an openable block for linking purposes.
   */
  void registerCustomOpenableBlock(Material material);

  /**
   * Unregisters a custom material from being treated as an openable block.
   */
  void unregisterCustomOpenableBlock(Material material);
}
