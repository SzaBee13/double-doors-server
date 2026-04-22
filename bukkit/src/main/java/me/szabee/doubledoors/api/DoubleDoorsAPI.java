package me.szabee.doubledoors.api;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Public integration API for DoubleDoors.
 */
public interface DoubleDoorsAPI {
  /**
   * Checks whether linked behavior is enabled for the given player.
   *
   * @param player the player to evaluate
   * @return true if linked behavior is enabled for the player
   */
  boolean isDoubleBehaviorEnabled(Player player);

  /**
   * Triggers linked behavior from an origin block.
   *
   * <p>This method must be called from the main server thread because it mutates block state.</p>
   *
   * @param origin the origin openable block
   * @param actor the player responsible for permission checks; null skips player-based checks
   * @return true if at least one linked block was changed
   */
  boolean triggerLinkedOpen(Block origin, @Nullable Player actor);

  /**
   * Registers a custom openable material for linked behavior.
   *
   * @param material the material to register
   */
  void registerCustomOpenableBlock(Material material);

  /**
   * Unregisters a custom openable material.
   *
   * @param material the material to unregister
   */
  void unregisterCustomOpenableBlock(Material material);
}
