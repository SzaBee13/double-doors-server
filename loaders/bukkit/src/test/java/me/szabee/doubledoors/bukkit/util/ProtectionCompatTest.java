package me.szabee.doubledoors.bukkit.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

class ProtectionCompatTest {

  /**
   * Fake GriefPrevention data store exposing the reflectively discovered
   * {@code getClaimAt} method.
   */
  static final class FakeDataStore {
    private final Object claim;

    FakeDataStore(Object claim) {
      this.claim = claim;
    }

    public Object getClaimAt(Location location, boolean includeNested) {
      return claim;
    }
  }

  /** Fake GriefPrevention claim with controllable check outcomes. */
  static final class FakeClaim {
    private String permissionResult = null;
    private String allowBuildResult = null;
    private Long id = null;

    public String checkPermission(Player player, me.ryanhamshire.GriefPrevention.ClaimPermission permission) {
      return permissionResult;
    }

    public String allowBuild(Player player, Material material) {
      return allowBuildResult;
    }

    public Long getID() {
      return id;
    }

    static FakeClaim denyingPermission() {
      FakeClaim claim = new FakeClaim();
      claim.permissionResult = "You lack access to this claim.";
      return claim;
    }

    static FakeClaim denyingBuild() {
      FakeClaim claim = new FakeClaim();
      claim.allowBuildResult = "You cannot build here.";
      return claim;
    }

    static FakeClaim withId(long value) {
      FakeClaim claim = new FakeClaim();
      claim.id = value;
      return claim;
    }
  }

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
    return config;
  }

  private static DoubleDoors plugin(PluginConfig config) {
    DoubleDoors plugin = mock(DoubleDoors.class);
    when(plugin.getPluginConfig()).thenReturn(config);
    when(plugin.getLogger()).thenReturn(Logger.getLogger("ProtectionCompatTest"));
    return plugin;
  }

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

  private static PluginManager managerWith(Plugin griefPreventionPlugin) {
    PluginManager pm = mock(PluginManager.class);
    when(pm.getPlugin("GriefPrevention")).thenReturn(griefPreventionPlugin);
    when(pm.getPlugin("WorldGuard")).thenReturn(null);
    return pm;
  }

  @Test
  void testLocationBlacklistDeniesLinkedDoor() {
    Block block = block("world", 10, 20, 30);
    PluginConfig config = config(
      PluginConfig.LocationMode.BLACKLIST,
      Set.of("world:10:20:30")
    );
    DoubleDoors plugin = plugin(config);
    PluginManager pm = managerWith(null);

    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager).thenReturn(pm);
      assertEquals(
        "location_blacklist",
        ProtectionCompat.explainLinkedDoorDeniedReason(plugin, null, block)
      );
      assertFalse(
        ProtectionCompat.canOpenLinkedDoor(plugin, null, block)
      );
    }
  }

  @Test
  void testLocationWhitelistDeniesUnlistedBlock() {
    Block block = block("world", 1, 2, 3);
    PluginConfig config = config(
      PluginConfig.LocationMode.WHITELIST,
      Set.of("world:9:9:9")
    );
    PluginManager pm = managerWith(null);

    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager).thenReturn(pm);
      assertEquals(
        "location_not_whitelisted",
        ProtectionCompat.explainLinkedDoorDeniedReason(plugin(config), null, block)
      );
    }
  }

  @Test
  void testNoIntegrationsAllowsWilderness() {
    Block block = block("world", 0, 64, 0);
    PluginConfig config = config(PluginConfig.LocationMode.BLACKLIST, Set.of());
    DoubleDoors plugin = plugin(config);
    Player player = mock(Player.class);
    PluginManager pm = managerWith(null);

    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager).thenReturn(pm);
      assertEquals("", ProtectionCompat.explainLinkedDoorDeniedReason(plugin, player, block));
      assertTrue(ProtectionCompat.canOpenLinkedDoor(plugin, player, block));
    }
  }

  @Test
  void testGriefPreventionReflectionFailureFailsClosed() {
    Block block = block("world", 5, 5, 5);
    PluginConfig config = config(PluginConfig.LocationMode.BLACKLIST, Set.of());
    DoubleDoors plugin = plugin(config);

    // dataStore field unreadable (null) -> fail closed.
    PluginManager missingDataStore = managerWith(griefPrevention(true));
    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager).thenReturn(missingDataStore);
      assertEquals(
        "griefprevention_unresolved",
        ProtectionCompat.explainLinkedDoorDeniedReason(plugin, null, block)
      );
    }

    // dataStore present but no compatible getClaimAt -> fail closed.
    PluginManager incompatibleApi =
      managerWith(griefPreventionWithDataStore(new Object()));
    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager).thenReturn(incompatibleApi);
      assertEquals(
        "griefprevention_unresolved",
        ProtectionCompat.explainLinkedDoorDeniedReason(plugin, null, block)
      );
    }
  }

  @Test
  void testGriefPreventionWildernessAllowsInteraction() {
    Block block = block("world", 7, 7, 7);
    PluginConfig config = config(PluginConfig.LocationMode.BLACKLIST, Set.of());
    DoubleDoors plugin = plugin(config);
    PluginManager pm =
      managerWith(griefPreventionWithDataStore(new FakeDataStore(null)));

    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager).thenReturn(pm);
      assertEquals("", ProtectionCompat.explainLinkedDoorDeniedReason(plugin, null, block));
    }
  }

  @Test
  void testGriefPreventionPermissionDenied() {
    Block block = block("world", 8, 8, 8);
    PluginConfig config = config(PluginConfig.LocationMode.BLACKLIST, Set.of());
    DoubleDoors plugin = plugin(config);
    Player player = mock(Player.class);
    PluginManager pm =
      managerWith(griefPreventionWithDataStore(new FakeDataStore(FakeClaim.denyingPermission())));

    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager).thenReturn(pm);
      assertEquals(
        "griefprevention_permission_denied",
        ProtectionCompat.explainLinkedDoorDeniedReason(plugin, player, block)
      );
    }
  }

  @Test
  void testGriefPreventionTrustedPlayerAllowedWithoutBuildRequirement() {
    Block block = block("world", 9, 9, 9);
    PluginConfig config = config(PluginConfig.LocationMode.BLACKLIST, Set.of());
    DoubleDoors plugin = plugin(config);
    Player player = mock(Player.class);
    PluginManager pm =
      managerWith(griefPreventionWithDataStore(new FakeDataStore(new FakeClaim())));

    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager).thenReturn(pm);
      assertEquals("", ProtectionCompat.explainLinkedDoorDeniedReason(plugin, player, block));
    }
  }

  @Test
  void testGriefPreventionBuildDeniedWhenRequired() {
    Block block = block("world", 11, 11, 11);
    PluginConfig config = config(PluginConfig.LocationMode.BLACKLIST, Set.of());
    when(config.isGriefPreventionRequireBuildForLinkedDoors()).thenReturn(true);
    DoubleDoors plugin = plugin(config);
    Player player = mock(Player.class);
    PluginManager pm =
      managerWith(griefPreventionWithDataStore(new FakeDataStore(FakeClaim.denyingBuild())));

    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager).thenReturn(pm);
      assertEquals(
        "griefprevention_build_denied",
        ProtectionCompat.explainLinkedDoorDeniedReason(plugin, player, block)
      );
    }
  }

  @Test
  void testDisabledGriefPreventionIsBypassed() {
    Block block = block("world", 12, 12, 12);
    PluginConfig config = config(PluginConfig.LocationMode.BLACKLIST, Set.of());
    PluginManager pm = managerWith(griefPrevention(false));

    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager).thenReturn(pm);
      assertEquals("", ProtectionCompat.explainLinkedDoorDeniedReason(plugin(config), null, block));
    }
  }

  @Test
  void testGetClaimIdAtExtractsIdAndFailsSoft() {
    Block block = block("world", 13, 13, 13);
    PluginConfig config = config(PluginConfig.LocationMode.BLACKLIST, Set.of());

    // No GriefPrevention installed.
    PluginManager absent = managerWith(null);
    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager).thenReturn(absent);
      assertEquals(-1L, ProtectionCompat.getClaimIdAt(plugin(config), block));
    }

    // Claim present with an ID.
    PluginManager present =
      managerWith(griefPreventionWithDataStore(new FakeDataStore(FakeClaim.withId(424242L))));
    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager).thenReturn(present);
      assertEquals(424242L, ProtectionCompat.getClaimIdAt(plugin(config), block));
    }
  }

  @Test
  void testIsClaimManagerAtDefaultsTrueWithoutIntegration() {
    Block block = block("world", 14, 14, 14);
    PluginConfig config = config(PluginConfig.LocationMode.BLACKLIST, Set.of());
    Player player = mock(Player.class);
    PluginManager pm = managerWith(null);

    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager).thenReturn(pm);
      assertTrue(ProtectionCompat.isClaimManagerAt(plugin(config), player, block));
    }
  }

  @Test
  void testIsClaimManagerAtFollowsBuildTrustAndFailsOpenOnError() {
    Block block = block("world", 15, 15, 15);
    PluginConfig config = config(PluginConfig.LocationMode.BLACKLIST, Set.of());
    DoubleDoors plugin = plugin(config);
    Player player = mock(Player.class);

    // allowBuild denies -> not a manager.
    PluginManager denying =
      managerWith(griefPreventionWithDataStore(new FakeDataStore(FakeClaim.denyingBuild())));
    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager).thenReturn(denying);
      assertFalse(ProtectionCompat.isClaimManagerAt(plugin, player, block));
    }

    // Reflection breaks -> documented fail-open for management checks.
    PluginManager broken =
      managerWith(griefPreventionWithDataStore(new Object()));
    try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
      bukkit.when(Bukkit::getPluginManager).thenReturn(broken);
      assertTrue(ProtectionCompat.isClaimManagerAt(plugin, player, block));
    }
  }
}
