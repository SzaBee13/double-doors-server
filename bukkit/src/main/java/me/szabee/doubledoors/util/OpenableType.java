package me.szabee.doubledoors.util;

import org.bukkit.Material;

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
   * @param material the block material
   * @return detected type, or null if unsupported
   */
  public static OpenableType fromMaterial(Material material) {
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
}