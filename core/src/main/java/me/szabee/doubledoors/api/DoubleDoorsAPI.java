package me.szabee.doubledoors.api;

/**
 * Bukkit-facing DoubleDoors API shared by supported server loaders.
 */
public interface DoubleDoorsAPI<BlockType, PlayerType, MaterialType> {
  /**
   * Returns whether linked-door behavior is enabled for the given player.
   *
   * @param player the player to check
   * @return whether linked-door behavior is enabled
   */
  boolean isDoubleBehaviorEnabled(PlayerType player);

  /**
   * Triggers linked open/close behavior for blocks connected to the origin.
   *
   * @param origin the block to start the linked toggle from
   * @param actor the player who triggered the action, or {@code null} for
   *              environmental triggers
   * @return whether at least one linked block was toggled
   */
  boolean triggerLinkedOpen(BlockType origin, PlayerType actor);

  /**
   * Registers a custom material as an openable block for linking purposes.
   *
   * @param material the material to register
   */
  void registerCustomOpenableBlock(MaterialType material);

  /**
   * Unregisters a custom material from being treated as an openable block.
   *
   * @param material the material to unregister
   */
  void unregisterCustomOpenableBlock(MaterialType material);
}
