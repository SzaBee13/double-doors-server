package me.szabee.doubledoors.util;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.TrapDoor;

/**
 * High-level categories for supported openable block types.
 */
public enum OpenableType {
  DOOR,
  FENCE_GATE,
  TRAPDOOR,
  CUSTOM;

  /**
   * Resolves an openable type from a material name suffix.
   *
   * @param material the block material, may be {@code null}
   * @return detected type, or {@code null} if unsupported or material is {@code null}
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
   * Resolves an openable type from Bukkit block data first, then material fallback.
   *
   * @param blockData the block data to inspect
   * @param material the backing block material
   * @return detected type, or null if unsupported
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
