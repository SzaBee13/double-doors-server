package me.szabee.doubledoors.bukkit.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import me.szabee.doubledoors.bukkit.DoubleDoors;
import me.szabee.doubledoors.bukkit.config.PlayerPreferences;
import me.szabee.doubledoors.bukkit.config.PluginConfig;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;

class FastStatsManagerTest {

  @Test
  void testNormalizeTokenAcceptsValidToken() {
    String token = "a".repeat(32);
    assertEquals(token, FastStatsManager.normalizeToken(token));
  }

  @Test
  void testNormalizeTokenStripsDashesAndLowercases() {
    String expected = "0123456789abcdef0123456789abcdef";
    assertEquals(
      expected,
      FastStatsManager.normalizeToken(
        "01234567-89AB-CDEF-0123-456789ABCDEF"
      )
    );
    assertEquals(expected, FastStatsManager.normalizeToken(" " + expected.toUpperCase(java.util.Locale.ROOT) + " "));
  }

  @Test
  void testNormalizeTokenRejectsInvalidTokens() {
    assertNull(FastStatsManager.normalizeToken(null));
    assertNull(FastStatsManager.normalizeToken(""));
    assertNull(FastStatsManager.normalizeToken("   "));
    assertNull(FastStatsManager.normalizeToken("short"));
    assertNull(FastStatsManager.normalizeToken("z".repeat(31)));
    assertNull(FastStatsManager.normalizeToken("!".repeat(32)));
  }

  @Test
  void testDataStorageTypeYmlWhenSqlDisabled() {
    PluginConfig config = mock(PluginConfig.class);
    when(config.isSqlEnabled()).thenReturn(false);
    when(config.getSqlJdbcUrl()).thenReturn("jdbc:mysql://localhost/db");
    assertEquals("yml", FastStatsManager.resolveDataStorageType(config));
  }

  @Test
  void testDataStorageTypeYmlWhenUrlMissing() {
    PluginConfig config = mock(PluginConfig.class);
    when(config.isSqlEnabled()).thenReturn(true);
    when(config.getSqlJdbcUrl()).thenReturn(null);
    assertEquals("yml", FastStatsManager.resolveDataStorageType(config));
  }

  @Test
  void testDataStorageTypeResolvesJdbcPrefixes() {
    PluginConfig config = mock(PluginConfig.class);
    when(config.isSqlEnabled()).thenReturn(true);
    when(config.getSqlJdbcUrl())
      .thenReturn("jdbc:sqlite:plugins/DoubleDoors/data.db")
      .thenReturn("jdbc:mysql://localhost/db")
      .thenReturn("jdbc:postgresql://localhost/db");
    assertEquals("sqlite", FastStatsManager.resolveDataStorageType(config));
    assertEquals("mysql", FastStatsManager.resolveDataStorageType(config));
    assertEquals("unknown", FastStatsManager.resolveDataStorageType(config));
  }

  @Test
  void testUpdateCheckerDetectsEnabledUpdaterOnly() {
    PluginManager pm = mock(PluginManager.class);

    Plugin disabledUpdater = mock(Plugin.class);
    when(disabledUpdater.isEnabled()).thenReturn(false);
    when(disabledUpdater.getName()).thenReturn("PluginUpdater");

    Plugin unrelated = mock(Plugin.class);
    when(unrelated.isEnabled()).thenReturn(true);
    when(unrelated.getName()).thenReturn("Vault");

    Plugin enabledUpdater = mock(Plugin.class);
    when(enabledUpdater.isEnabled()).thenReturn(true);
    when(enabledUpdater.getName()).thenReturn("pluginupdaterplugin");

    when(pm.getPlugins())
      .thenReturn(new Plugin[] { disabledUpdater })
      .thenReturn(new Plugin[] { unrelated })
      .thenReturn(new Plugin[] { unrelated, enabledUpdater });

    assertEquals("on", FastStatsManager.resolveUpdateChecker(pm));
    assertEquals("on", FastStatsManager.resolveUpdateChecker(pm));
    assertEquals("pluginupdater", FastStatsManager.resolveUpdateChecker(pm));
  }

  @Test
  void testLocaleSnapshotEmptyWhenDisabled() {
    DoubleDoors plugin = mock(DoubleDoors.class);
    PluginConfig config = mock(PluginConfig.class);
    when(config.isPerPlayerLocaleEnabled()).thenReturn(false);

    FastStatsManager manager = new FastStatsManager(plugin);
    manager.refreshPerPlayerLocaleSnapshot(config);

    assertArrayEquals(new String[0], manager.currentPerPlayerLocales());
  }

  @Test
  void testLocaleSnapshotEmptyBeforePreferencesReady() {
    DoubleDoors plugin = mock(DoubleDoors.class);
    PluginConfig config = mock(PluginConfig.class);
    when(config.isPerPlayerLocaleEnabled()).thenReturn(true);
    when(plugin.getPlayerPreferences()).thenReturn(null);

    FastStatsManager manager = new FastStatsManager(plugin);
    manager.refreshPerPlayerLocaleSnapshot(config);

    assertArrayEquals(new String[0], manager.currentPerPlayerLocales());
  }

  @Test
  void testLocaleSnapshotFallsBackToServerLanguage() {
    Server server = mock(Server.class);
    DoubleDoors plugin = mock(DoubleDoors.class);
    when(plugin.getServer()).thenReturn(server);

    PlayerPreferences prefs = mock(PlayerPreferences.class);
    when(plugin.getPlayerPreferences()).thenReturn(prefs);

    UUID german = UUID.randomUUID();
    UUID unset = UUID.randomUUID();
    Player germanPlayer = mock(Player.class);
    when(germanPlayer.getUniqueId()).thenReturn(german);
    Player unsetPlayer = mock(Player.class);
    when(unsetPlayer.getUniqueId()).thenReturn(unset);
    when(server.getOnlinePlayers()).thenAnswer(invocation -> List.of(germanPlayer, unsetPlayer));

    when(prefs.getLocale(german)).thenReturn("de_DE");
    when(prefs.getLocale(unset)).thenReturn("  ");

    PluginConfig config = mock(PluginConfig.class);
    when(config.isPerPlayerLocaleEnabled()).thenReturn(true);
    when(config.getLanguage()).thenReturn("en_US");

    FastStatsManager manager = new FastStatsManager(plugin);
    manager.refreshPerPlayerLocaleSnapshot(config);

    assertArrayEquals(
      new String[] { "de_DE", "en_US" },
      manager.currentPerPlayerLocales()
    );
  }
}
