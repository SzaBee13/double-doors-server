package me.szabee.doubledoors.bukkit.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import me.szabee.doubledoors.bukkit.DoubleDoors;
import me.szabee.doubledoors.bukkit.util.SchedulerBridge;
import me.szabee.doubledoors.storage.SharedSqlStorage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

class ClaimSettingsTest {

  @TempDir
  Path tempDir;

  private DoubleDoors plugin(File dataFolder, PluginConfig config, SharedSqlStorage storage) {
    DoubleDoors plugin = mock(DoubleDoors.class);
    when(plugin.getDataFolder()).thenReturn(dataFolder);
    when(plugin.getPluginConfig()).thenReturn(config);
    when(plugin.getSqlStorage()).thenReturn(storage);
    return plugin;
  }

  @Test
  void loadsNumericYamlClaimIdsAndSavesToggles() throws Exception {
    File file = tempDir.resolve("claims.yml").toFile();
    YamlConfiguration yaml = new YamlConfiguration();
    yaml.set("villagersBlocked", List.of(12, 34L, "not-a-number"));
    yaml.save(file);
    PluginConfig config = mock(PluginConfig.class);
    when(config.isSqlEnabled()).thenReturn(false);
    ClaimSettings settings = new ClaimSettings(plugin(tempDir.toFile(), config, null));

    assertTrue(settings.isVillagersBlocked(12));
    assertTrue(settings.isVillagersBlocked(34));
    assertFalse(settings.isVillagersBlocked(56));

    AtomicReference<Runnable> scheduled = new AtomicReference<>();
    try (MockedStatic<SchedulerBridge> scheduler = mockStatic(SchedulerBridge.class)) {
      scheduler.when(() -> SchedulerBridge.runAsync(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> {
          scheduled.set(invocation.getArgument(1));
          return null;
        });
      assertTrue(settings.toggleVillagersBlocked(56));
      scheduled.get().run();
    }

    YamlConfiguration saved = YamlConfiguration.loadConfiguration(file);
    assertTrue(saved.getLongList("villagersBlocked").containsAll(List.of(12L, 34L, 56L)));

    try (MockedStatic<SchedulerBridge> scheduler = mockStatic(SchedulerBridge.class)) {
      scheduler.when(() -> SchedulerBridge.runAsync(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> {
          scheduled.set(invocation.getArgument(1));
          return null;
        });
      assertFalse(settings.toggleVillagersBlocked(56));
      scheduled.get().run();
    }
    assertFalse(YamlConfiguration.loadConfiguration(file).getLongList("villagersBlocked").contains(56L));
  }

  @Test
  void loadsAndSavesClaimSettingsThroughSql() {
    SharedSqlStorage storage = mock(SharedSqlStorage.class);
    when(storage.loadVillagersBlockedClaims()).thenReturn(Set.of(91L));
    when(storage.setVillagersBlocked(92, true)).thenReturn(true);
    PluginConfig config = mock(PluginConfig.class);
    when(config.isSqlEnabled()).thenReturn(true);
    ClaimSettings settings = new ClaimSettings(plugin(tempDir.toFile(), config, storage));

    assertTrue(settings.isVillagersBlocked(91));

    AtomicReference<Runnable> scheduled = new AtomicReference<>();
    try (MockedStatic<SchedulerBridge> scheduler = mockStatic(SchedulerBridge.class)) {
      scheduler.when(() -> SchedulerBridge.runAsync(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenAnswer(invocation -> {
          scheduled.set(invocation.getArgument(1));
          return null;
        });
      assertTrue(settings.toggleVillagersBlocked(92));
      scheduled.get().run();
    }
    org.mockito.Mockito.verify(storage).setVillagersBlocked(92, true);
  }
}
