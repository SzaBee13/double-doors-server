package me.szabee.doubledoors.bukkit.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    when(pm.getPlugin("WorldGuard")).thenReturn(plugin);
    when(pm.getPlugin("GriefPrevention")).thenReturn(null);
    return pm;
  }

  @Test
  void testIsLocationAllowed() {
    Block block = block("world", 1, 2, 3);
    PluginConfig config = config(PluginConfig.LocationMode.BLACKLIST, Set.of("world:1:2:3"));
    DoubleDoors plugin = plugin(config);
    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager).thenReturn(managerWith(null));
      assertTrue(ProtectionCompat.isLocationAllowed(plugin, block));
    }
  }

  @Test
  void testIsLocationAllowedWhitelistDeniesUnlisted() {
    Block block = block("world", 4, 5, 6);
    PluginConfig config = config(PluginConfig.LocationMode.WHITELIST, Set.of("world:1:2:3"));
    DoubleDoors plugin = plugin(config);
    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager).thenReturn(managerWith(null));
      assertFalse(ProtectionCompat.isLocationAllowed(plugin, block));
    }
  }

  /** Stub WorldGuard plugin that denies builds via legacy canBuild method. */
  public static class StubWorldGuardPlugin implements Plugin {
    @Override public void onLoad() {}
    @Override public void onEnable() {}
    @Override public void onDisable() {}
    @Override public String getName() { return "WorldGuard"; }
    @Override public boolean isEnabled() { return true; }
    @Override public boolean isNaggable() { return false; }
    // other Plugin methods default to null / false
    @Override public java.io.File getDataFolder() { return null; }
    @Override public org.bukkit.Server getServer() { return null; }
    @Override public org.bukkit.plugin.PluginDescriptionFile getDescription() { return null; }
    @Override public org.bukkit.plugin.PluginLoader getPluginLoader() { return null; }
    @Override public org.bukkit.command.PluginCommand getPluginCommand(String name) { return null; }
    @Override public java.util.logging.Logger getLogger() { return Logger.getLogger("StubWG"); }
    @Override public org.bukkit.command.Command getCommand(String name) { return null; }
    @Override public java.util.List<String> getAuthors() { return null; }
    @Override public java.util.List<String> getContributors() { return null; }
    @Override public org.bukkit.plugin.Plugin getProvidingPlugin() { return this; }
    @Override public java.util.Map<java.lang.String, org.bukkit.command.Command> getCommands() { return null; }
    @Override public org.bukkit.plugin.Plugin getParent() { return null; }
    @Override public java.util.Set<org.bukkit.plugin.Plugin> getDependants() { return null; }
  }

  // Minimal stub matching the class used via reflection.
  // Package must be exactly as expected by ProtectionCompat.
  @SuppressWarnings("unused")
  public static class com {
    public static class sk89q {
      public static class worldguard {
        public static class bukkit {
          public static class WorldGuardPlugin {
            public static WorldGuardPlugin inst() { return new WorldGuardPlugin(); }
            public Boolean canBuild(org.bukkit.entity.Player player, org.bukkit.Location loc) { return false; }
          }
        }
      }
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
    // Ensure Bukkit.getPluginManager returns this plugin for "WorldGuard".
    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      when(bukkit.invoke(Bukkit::getPluginManager)).thenReturn(pm);
      when(pm.getPlugin("WorldGuard")).thenReturn(wgPlugin);
      when(pm.getPlugin("GriefPrevention")).thenReturn(null);
      // Use the stub class via reflection. Ensure class is loadable.
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
    FakeDataStore ds = new FakeDataStore(new ClaimWithField(12345L));
    PluginManager pm = managerWith(griefPreventionWithDataStore(ds));
    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager).thenReturn(pm);
      long id = ProtectionCompat.getClaimIdAt(plugin, block);
      assertEquals(12345L, id);
    }
  }

  // Helper methods similar to those in other tests.
  private static Plugin griefPrevention(boolean enabled) {
    Plugin gp = mock(Plugin.class);
    when(gp.isEnabled()).thenReturn(enabled);
    return gp;
  }

  private static Plugin griefPreventionWithDataStore(Object dataStore) {
    Plugin gp = griefPrevention(true);
    try {
      // Use reflection to set public field dataStore on the mock.
      java.lang.reflect.Field f = gp.getClass().getDeclaredField("dataStore");
    } catch (NoSuchFieldException e) {
      // Ignore; we'll rely on the mock handling via when.
    }
    // Mockito allows setting fields directly on mock objects.
    ((FakeGriefPreventionBase) gp).dataStore = dataStore;
    return gp;
  }
}
