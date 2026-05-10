package me.szabee.doubledoors.util;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class OpenableTypeTest {

  @Test
  void testFromMaterial() {
    assertEquals(OpenableType.DOOR, OpenableType.fromMaterial(Material.OAK_DOOR));
    assertEquals(OpenableType.DOOR, OpenableType.fromMaterial(Material.IRON_DOOR));
    assertEquals(OpenableType.FENCE_GATE, OpenableType.fromMaterial(Material.OAK_FENCE_GATE));
    assertEquals(OpenableType.TRAPDOOR, OpenableType.fromMaterial(Material.OAK_TRAPDOOR));
    assertNull(OpenableType.fromMaterial(Material.OAK_PLANKS));
  }
}
