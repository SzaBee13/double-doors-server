package me.szabee.doubledoors.storage;

import me.szabee.doubledoors.DoubleDoors;
import me.szabee.doubledoors.config.PluginConfig;

/**
 * Bukkit adapter for shared SQL storage configuration.
 */
public final class BukkitSharedSqlStorage extends SharedSqlStorage {
  /**
   * Creates the storage adapter from Bukkit plugin config.
   *
   * @param plugin Bukkit plugin instance
   * @param config Bukkit config wrapper
   */
  public BukkitSharedSqlStorage(DoubleDoors plugin, PluginConfig config) {
    super(plugin.getLogger(), config.getSqlJdbcUrl(), config.getSqlUsername(), config.getSqlPassword());
  }
}
