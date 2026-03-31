package me.szabee.doubledoors.migration;

import me.szabee.doubledoors.DoubleDoors;
import me.szabee.doubledoors.storage.SharedSqlStorage;
import java.io.File;
import java.util.List;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Migrates legacy YAML persistence files into SQL tables.
 */
public final class YamlToSqlMigrator {

  /** Migration key stored in dd_meta. */
  public static final String MIGRATION_KEY = "yaml_to_sql_v1";

  private YamlToSqlMigrator() {
  }

  /**
   * Runs a one-time migration from YAML files into SQL.
   *
   * @param plugin the plugin instance
   * @param sqlStorage initialized SQL storage
   */
  public static void migrateIfNeeded(DoubleDoors plugin, SharedSqlStorage sqlStorage) {
    if (sqlStorage.isMigrationDone(MIGRATION_KEY)) {
      return;
    }

    File playersFile = new File(plugin.getDataFolder(), "players.yml");
    if (playersFile.exists()) {
      migratePlayers(plugin, sqlStorage, playersFile);
    }

    File claimsFile = new File(plugin.getDataFolder(), "claims.yml");
    if (claimsFile.exists()) {
      migrateClaims(plugin, sqlStorage, claimsFile);
    }

    sqlStorage.markMigrationDone(MIGRATION_KEY);
    plugin.getLogger().info("DoubleDoors migrated YAML storage into SQL (" + MIGRATION_KEY + ").");
  }

  private static void migratePlayers(DoubleDoors plugin, SharedSqlStorage sqlStorage, File playersFile) {
    YamlConfiguration data = YamlConfiguration.loadConfiguration(playersFile);
    int migrated = 0;
    for (String key : data.getKeys(false)) {
      try {
        UUID uuid = UUID.fromString(key);
        boolean enabled = data.getBoolean(key + ".enabled", true);
        boolean doors = data.getBoolean(key + ".enableDoors", true);
        boolean gates = data.getBoolean(key + ".enableFenceGates", true);
        boolean trapdoors = data.getBoolean(key + ".enableTrapdoors", true);
        sqlStorage.savePlayerPreference(uuid, new SharedSqlStorage.SqlPlayerPref(enabled, doors, gates, trapdoors));
        migrated++;
      } catch (IllegalArgumentException ignored) {
        // Ignore unexpected top-level keys.
      }
    }
    plugin.getLogger().info("DoubleDoors migrated " + migrated + " player preference rows from players.yml.");
  }

  private static void migrateClaims(DoubleDoors plugin, SharedSqlStorage sqlStorage, File claimsFile) {
    YamlConfiguration data = YamlConfiguration.loadConfiguration(claimsFile);
    List<?> blocked = data.getList("villagersBlocked");
    int migrated = 0;
    if (blocked != null) {
      for (Object entry : blocked) {
        if (entry instanceof Number number) {
          sqlStorage.setVillagersBlocked(number.longValue(), true);
          migrated++;
        }
      }
    }
    plugin.getLogger().info("DoubleDoors migrated " + migrated + " claim rows from claims.yml.");
  }
}
