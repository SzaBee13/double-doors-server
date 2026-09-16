package me.szabee.doubledoors.bukkit.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.logging.Logger;
import me.szabee.doubledoors.bukkit.DoubleDoors;
import me.szabee.doubledoors.bukkit.config.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Additional tests for {@link ProtectionCompat} to increase coverage.
 */
class ProtectionCompatAdditionalTest {

  private static Block block(String worldName, int x, int y, int z) {
    World world = mock(World.class);
    when(world.getName()).thenReturn(worldName);
    Block block = mock(Block.class);
    when(block.getWorld()).thenReturn(world);
    when(block.getX()).thenReturn(x);
    when(block.getY()).thenReturn(y);
    when(block.getZ()).thenReturn(z);
    when(block.getLocation()).thenReturn(mock(Location.class));
    when(block.getType()).thenReturn(Material.OAK_DOOR);
    return block;
  }

  private static PluginConfig config(PluginConfig.LocationMode mode, Set<String> entries) {
    PluginConfig config = mock(PluginConfig.class);
    when(config.getLocationMode()).thenReturn(mode);
    when(config.getLocationEntries()).thenReturn(entries);
    when(config.getWorldGuardRegionMode()).thenReturn(PluginConfig.RegionMode.BLACKLIST);
    when(config.getWorldGuardRegionIds()).thenReturn(Set.of());
    when(config.isGriefPreventionRequireBuildForLinkedDoors()).thenReturn(false);
    when(config.isWorldGuardRespectBuildPermission()).thenReturn(false);
    when(config.isWorldGuardRespectUseFlag()).thenReturn(false);
    when(config.getWorldGuardCustomFlag()).thenReturn("");
    return config;
  }

  private static DoubleDoors plugin(PluginConfig config) {
    DoubleDoors plugin = mock(DoubleDoors.class);
    when(plugin.getPluginConfig()).thenReturn(config);
    when(plugin.getLogger()).thenReturn(Logger.getLogger("ProtectionCompatAdditionalTest"));
    return plugin;
  }

  private static PluginManager managerWith(Plugin plugin) {
    PluginManager pm = mock(PluginManager.class);
    when(pm.getPlugin("GriefPrevention")).thenReturn(plugin);
    when(pm.getPlugin("WorldGuard")).thenReturn(null);
    return pm;
  }

  @Test
  void testIsLocationAllowed() {
    Block block = block("world", 1, 2, 3);
    PluginConfig config = config(PluginConfig.LocationMode.BLACKLIST, Set.of("world:1:2:3"));
    DoubleDoors plugin = plugin(config);
    PluginManager pm = managerWith(null);
    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager).thenReturn(pm);
      assertFalse(ProtectionCompat.isLocationAllowed(plugin, block));
    }
  }

  @Test
  void testIsLocationAllowedWhitelistDeniesUnlisted() {
    Block block = block("world", 4, 5, 6);
    PluginConfig config = config(PluginConfig.LocationMode.WHITELIST, Set.of("world:1:2:3"));
    DoubleDoors plugin = plugin(config);
    PluginManager pm = managerWith(null);
    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager).thenReturn(pm);
      assertFalse(ProtectionCompat.isLocationAllowed(plugin, block));
    }
  }

  @Test
  void testWorldGuardBuildDenied() {
    Block block = block("world", 0, 0, 0);
    PluginConfig config = config(PluginConfig.LocationMode.BLACKLIST, Set.of());
    // Enable respect for build permission.
    when(config.isWorldGuardRespectBuildPermission()).thenReturn(true);
    DoubleDoors plugin = plugin(config);

    PluginManager pm = mock(PluginManager.class);
    // Return a stub plugin instance that will be identified as WorldGuard.
    Plugin wgPlugin = mock(Plugin.class);
    when(wgPlugin.getName()).thenReturn("WorldGuard");
    when(wgPlugin.isEnabled()).thenReturn(true);
    when(pm.getPlugin("WorldGuard")).thenReturn(wgPlugin);
    when(pm.getPlugin("GriefPrevention")).thenReturn(null);
    // Ensure Bukkit.getPluginManager returns this plugin for "WorldGuard".
    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager).thenReturn(pm);
      String reason = ProtectionCompat.explainLinkedDoorDeniedReason(plugin, mock(Player.class), block);
      assertEquals("worldguard_build_denied", reason);
    }
  }

  /** Claim class with public field `id` used for fallback extraction. */
  public static class ClaimWithField {
    public Long id;
    public ClaimWithField(Long id) { this.id = id; }
  }

  @Test
  void testGetClaimIdAtFieldFallback() {
    Block block = block("world", 1, 1, 1);
    PluginConfig config = config(PluginConfig.LocationMode.BLACKLIST, Set.of());
    DoubleDoors plugin = plugin(config);
    // DataStore returning a claim that only has a public field.
    ProtectionCompatTest.FakeDataStore ds = new ProtectionCompatTest.FakeDataStore(new ClaimWithField(12345L));
    PluginManager pm = managerWith(griefPreventionWithDataStore(ds));
    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager).thenReturn(pm);
      long id = ProtectionCompat.getClaimIdAt(plugin, block);
      assertEquals(12345L, id);
    }
  }

  // Helper methods similar to those in other tests.
  private static FakeGriefPreventionBase griefPrevention(boolean enabled) {
    FakeGriefPreventionBase gp = mock(FakeGriefPreventionBase.class);
    when(gp.isEnabled()).thenReturn(enabled);
    return gp;
  }

  private static Plugin griefPreventionWithDataStore(Object dataStore) {
    FakeGriefPreventionBase gp = griefPrevention(true);
    gp.dataStore = dataStore;
    return gp;
  }
}
