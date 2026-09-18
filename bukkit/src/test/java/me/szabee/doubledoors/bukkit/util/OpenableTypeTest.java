package me.szabee.doubledoors.bukkit.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

final class OpenableTypeTest {

  @Test
  void testFromMaterial() {
    assertEquals(
      OpenableType.DOOR,
      OpenableType.fromMaterial(Material.OAK_DOOR)
    );
    assertEquals(
      OpenableType.DOOR,
      OpenableType.fromMaterial(Material.IRON_DOOR)
    );
    assertEquals(
      OpenableType.FENCE_GATE,
      OpenableType.fromMaterial(Material.OAK_FENCE_GATE)
    );
    assertEquals(
      OpenableType.TRAPDOOR,
      OpenableType.fromMaterial(Material.OAK_TRAPDOOR)
    );
    assertNull(OpenableType.fromMaterial(Material.OAK_PLANKS));
  }

  @Test
  void testPoplarWoodTypes() {
    Material poplarDoor = mock(Material.class);
    when(poplarDoor.name()).thenReturn("POPLAR_DOOR");
    assertEquals(OpenableType.DOOR, OpenableType.fromMaterial(poplarDoor));

    Material poplarGate = mock(Material.class);
    when(poplarGate.name()).thenReturn("POPLAR_FENCE_GATE");
    assertEquals(OpenableType.FENCE_GATE, OpenableType.fromMaterial(poplarGate));

    Material poplarTrapdoor = mock(Material.class);
    when(poplarTrapdoor.name()).thenReturn("POPLAR_TRAPDOOR");
    assertEquals(OpenableType.TRAPDOOR, OpenableType.fromMaterial(poplarTrapdoor));
  }
}
