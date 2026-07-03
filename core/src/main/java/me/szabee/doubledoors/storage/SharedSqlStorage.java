package me.szabee.doubledoors.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Shared SQL storage used by Bukkit and proxy components.
 */
public class SharedSqlStorage {
  private final Logger logger;
  private final String jdbcUrl;
  private final String username;
  private final String password;

  public SharedSqlStorage(Logger logger, String jdbcUrl, String username, String password) {
    this.logger = Objects.requireNonNull(logger);
    this.jdbcUrl = Objects.requireNonNull(jdbcUrl);
    this.username = username == null ? "" : username;
    this.password = password == null ? "" : password;
  }

  /**
   * Creates the required tables and adds any missing columns (safe to call repeatedly).
   *
   * @throws IllegalStateException if schema initialization fails
   */
  public void initializeSchema() {
    try {
      executeStatement("CREATE TABLE IF NOT EXISTS dd_player_preferences ("
        + "player_uuid VARCHAR(36) PRIMARY KEY,"
        + "enabled BOOLEAN NOT NULL,"
        + "enable_doors BOOLEAN NOT NULL,"
        + "enable_fence_gates BOOLEAN NOT NULL,"
        + "enable_trapdoors BOOLEAN NOT NULL,"
        + "enable_auto_close BOOLEAN NOT NULL DEFAULT TRUE,"
        + "enable_knock_sound BOOLEAN NOT NULL DEFAULT TRUE,"
        + "knock_volume DOUBLE NOT NULL DEFAULT 1.0,"
        + "locale VARCHAR(32) NOT NULL DEFAULT ''"
        + ")");
      executeAlterAddColumnIfAbsent("ALTER TABLE dd_player_preferences ADD COLUMN enable_auto_close BOOLEAN NOT NULL DEFAULT TRUE");
      executeAlterAddColumnIfAbsent("ALTER TABLE dd_player_preferences ADD COLUMN enable_knock_sound BOOLEAN NOT NULL DEFAULT TRUE");
      executeAlterAddColumnIfAbsent("ALTER TABLE dd_player_preferences ADD COLUMN knock_volume DOUBLE NOT NULL DEFAULT 1.0");
      executeAlterAddColumnIfAbsent("ALTER TABLE dd_player_preferences ADD COLUMN locale VARCHAR(32) NOT NULL DEFAULT ''");
      executeStatement("CREATE TABLE IF NOT EXISTS dd_claim_settings (claim_id BIGINT PRIMARY KEY, villagers_blocked BOOLEAN NOT NULL)");
      executeStatement("CREATE TABLE IF NOT EXISTS dd_proxy_presence ("
        + "proxy_id VARCHAR(128) PRIMARY KEY,"
        + "platform VARCHAR(32) NOT NULL,"
        + "last_seen_epoch_ms BIGINT NOT NULL,"
        + "has_geyser BOOLEAN NOT NULL DEFAULT FALSE,"
        + "has_floodgate BOOLEAN NOT NULL DEFAULT FALSE"
        + ")");
      executeAlterAddColumnIfAbsent("ALTER TABLE dd_proxy_presence ADD COLUMN has_geyser BOOLEAN NOT NULL DEFAULT FALSE");
      executeAlterAddColumnIfAbsent("ALTER TABLE dd_proxy_presence ADD COLUMN has_floodgate BOOLEAN NOT NULL DEFAULT FALSE");
      executeStatement("CREATE TABLE IF NOT EXISTS dd_meta (meta_key VARCHAR(128) PRIMARY KEY, meta_value VARCHAR(255) NOT NULL)");
    } catch (SQLException e) {
      throw new IllegalStateException("Could not initialize SQL schema", e);
    }
  }

  /**
   * Loads all stored player preferences.
   *
   * @return a map of UUID to player preference (never null)
   */
  public Map<UUID, SqlPlayerPref> loadAllPlayerPreferences() {
    Map<UUID, SqlPlayerPref> result = new HashMap<>();
    String sql = "SELECT player_uuid, enabled, enable_doors, enable_fence_gates, enable_trapdoors, enable_auto_close, enable_knock_sound, knock_volume, locale FROM dd_player_preferences";
    try (Connection connection = openConnection(); Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
      while (rs.next()) {
        try {
          UUID uuid = UUID.fromString(rs.getString("player_uuid"));
          result.put(uuid, new SqlPlayerPref(rs.getBoolean("enabled"), rs.getBoolean("enable_doors"), rs.getBoolean("enable_fence_gates"), rs.getBoolean("enable_trapdoors"), rs.getBoolean("enable_auto_close"), rs.getBoolean("enable_knock_sound"), rs.getDouble("knock_volume"), rs.getString("locale")));
        } catch (IllegalArgumentException e) {
          logger.fine("Skipping malformed player UUID in SQL storage: " + e.getMessage());
        }
      }
    } catch (SQLException e) {
      logger.warning(String.format("Could not load player preferences from SQL: %s", e.getMessage()));
    }
    return result;
  }

  /**
   * Saves a player preference (insert or update).
   *
   * @return true on success, false on failure
   */
  public boolean savePlayerPreference(UUID uuid, SqlPlayerPref pref) {
    String sql = "INSERT INTO dd_player_preferences (player_uuid, enabled, enable_doors, enable_fence_gates, enable_trapdoors, enable_auto_close, enable_knock_sound, knock_volume, locale) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE enabled=VALUES(enabled), enable_doors=VALUES(enable_doors), enable_fence_gates=VALUES(enable_fence_gates), enable_trapdoors=VALUES(enable_trapdoors), enable_auto_close=VALUES(enable_auto_close), enable_knock_sound=VALUES(enable_knock_sound), knock_volume=VALUES(knock_volume), locale=VALUES(locale)";
    try (Connection connection = openConnection(); PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, uuid.toString());
      stmt.setBoolean(2, pref.enabled());
      stmt.setBoolean(3, pref.enableDoors());
      stmt.setBoolean(4, pref.enableFenceGates());
      stmt.setBoolean(5, pref.enableTrapdoors());
      stmt.setBoolean(6, pref.enableAutoClose());
      stmt.setBoolean(7, pref.enableKnockSound());
      stmt.setDouble(8, pref.knockVolume());
      stmt.setString(9, pref.locale());
      stmt.executeUpdate();
    } catch (SQLException e) {
      logger.warning(String.format("Could not save player preference to SQL: %s", e.getMessage()));
      return false;
    }
    return true;
  }

  /**
   * Loads all claim IDs for which villager door interactions are blocked.
   */
  public Set<Long> loadVillagersBlockedClaims() {
    Set<Long> blocked = new HashSet<>();
    String sql = "SELECT claim_id FROM dd_claim_settings WHERE villagers_blocked=?";
    try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setBoolean(1, true);
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          blocked.add(rs.getLong("claim_id"));
        }
      }
    } catch (SQLException e) {
      logger.warning(String.format("Could not load claim settings from SQL: %s", e.getMessage()));
    }
    return blocked;
  }

  /**
   * Sets whether villager door interactions are blocked for the given claim.
   *
   * @return true on success, false on failure
   */
  public boolean setVillagersBlocked(long claimId, boolean blocked) {
    String sql = "INSERT INTO dd_claim_settings (claim_id, villagers_blocked) VALUES (?, ?) ON DUPLICATE KEY UPDATE villagers_blocked=VALUES(villagers_blocked)";
    try (Connection connection = openConnection(); PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setLong(1, claimId);
      stmt.setBoolean(2, blocked);
      stmt.executeUpdate();
    } catch (SQLException e) {
      logger.warning(String.format("Could not save claim setting to SQL: %s", e.getMessage()));
      return false;
    }
    return true;
  }

  /**
   * Checks whether a migration has been completed.
   */
  public boolean isMigrationDone(String migrationKey) {
    try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement("SELECT meta_value FROM dd_meta WHERE meta_key=?")) {
      statement.setString(1, migrationKey);
      try (ResultSet rs = statement.executeQuery()) {
        return rs.next() && "done".equalsIgnoreCase(rs.getString("meta_value"));
      }
    } catch (SQLException e) {
      logger.warning(String.format("Could not read SQL migration metadata: %s", e.getMessage()));
      return false;
    }
  }

  /**
   * Marks a migration as completed (idempotent).
   */
  public void markMigrationDone(String migrationKey) {
    String sql = "INSERT INTO dd_meta (meta_key, meta_value) VALUES (?, 'done') ON DUPLICATE KEY UPDATE meta_value='done'";
    try (Connection connection = openConnection(); PreparedStatement stmt = connection.prepareStatement(sql)) {
      stmt.setString(1, migrationKey);
      stmt.executeUpdate();
    } catch (SQLException e) {
      logger.warning(String.format("Could not write SQL migration metadata: %s", e.getMessage()));
    }
  }

  /**
   * Returns whether any proxy has sent a heartbeat within the given age.
   */
  public boolean hasRecentProxyHeartbeat(long maxAgeMillis) {
    long threshold = System.currentTimeMillis() - maxAgeMillis;
    try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM dd_proxy_presence WHERE last_seen_epoch_ms >= ? LIMIT 1")) {
      statement.setLong(1, threshold);
      try (ResultSet rs = statement.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      logger.warning(String.format("Could not read proxy heartbeat from SQL: %s", e.getMessage()));
      return false;
    }
  }

  /**
   * Returns whether any proxy with Geyser/Floodgate has sent a heartbeat within the given age.
   */
  public boolean hasRecentProxyGeyserBridge(long maxAgeMillis) {
    long threshold = System.currentTimeMillis() - maxAgeMillis;
    try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM dd_proxy_presence WHERE last_seen_epoch_ms >= ? AND (has_geyser = ? OR has_floodgate = ?) LIMIT 1")) {
      statement.setLong(1, threshold);
      statement.setBoolean(2, true);
      statement.setBoolean(3, true);
      try (ResultSet rs = statement.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      logger.warning(String.format("Could not read proxy Geyser/Floodgate heartbeat from SQL: %s", e.getMessage()));
      return false;
    }
  }

  private Connection openConnection() throws SQLException {
    return username.isBlank() ? DriverManager.getConnection(jdbcUrl) : DriverManager.getConnection(jdbcUrl, username, password);
  }

  private void executeStatement(String sql) throws SQLException {
    try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
      statement.executeUpdate(sql);
    }
  }

  private void executeAlterAddColumnIfAbsent(String sql) throws SQLException {
    try {
      executeStatement(sql);
    } catch (SQLException e) {
      String message = e.getMessage();
      String normalizedMessage = message == null ? "" : message.toLowerCase();
      if (e.getErrorCode() == 1060 || normalizedMessage.contains("duplicate column") || normalizedMessage.contains("already exists")) {
        return;
      }
      throw e;
    }
  }

  public record SqlPlayerPref(boolean enabled, boolean enableDoors, boolean enableFenceGates, boolean enableTrapdoors, boolean enableAutoClose, boolean enableKnockSound, double knockVolume, String locale) {
  }
}
