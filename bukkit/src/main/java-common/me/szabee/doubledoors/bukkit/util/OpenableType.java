package me.szabee.doubledoors.bukkit.util;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.TrapDoor;

/**
 * Types of openable blocks supported by DoubleDoors linking.
 */
public enum OpenableType {
  DOOR,
  FENCE_GATE,
  TRAPDOOR,
  CUSTOM;

  /**
   * Returns the matching {@link OpenableType} for the given material,
   * or {@code null} if the material is not an openable block.
   */
  public static OpenableType fromMaterial(Material material) {
    if (material == null) {
      return null;
    }
    String name = material.name();
    if (name.endsWith("_DOOR")) {
      return DOOR;
    }
    if (name.endsWith("_FENCE_GATE")) {
      return FENCE_GATE;
    }
    if (name.endsWith("_TRAPDOOR")) {
      return TRAPDOOR;
    }
    return null;
  }

  /**
   * Resolves the openable type from block data, falling back to material-based detection.
   *
   * @param blockData the block data to inspect
   * @param material  fallback material when block data alone is insufficient
   * @return the resolved type, or {@code null} if neither source matches
   */
  public static OpenableType fromBlockData(BlockData blockData, Material material) {
    if (blockData instanceof Door) {
      return DOOR;
    }
    if (blockData instanceof Gate) {
      return FENCE_GATE;
    }
    if (blockData instanceof TrapDoor) {
      return TRAPDOOR;
    }
    return fromMaterial(material);
  }
}
