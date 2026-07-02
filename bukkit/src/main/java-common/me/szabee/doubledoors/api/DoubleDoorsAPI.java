package me.szabee.doubledoors.api;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Bukkit-facing DoubleDoors API.
 */
public interface DoubleDoorsAPI {
  boolean isDoubleBehaviorEnabled(Player player);

  boolean triggerLinkedOpen(Block origin, @Nullable Player actor);

  void registerCustomOpenableBlock(Material material);

  void unregisterCustomOpenableBlock(Material material);
}
