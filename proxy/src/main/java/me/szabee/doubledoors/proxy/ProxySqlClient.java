package me.szabee.doubledoors.proxy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQL client used by the proxy module for heartbeat writes.
 */
public final class ProxySqlClient {

  private final String jdbcUrl;
  private final String username;
  private final String password;

  /**
   * Creates a SQL client.
   *
   * @param jdbcUrl JDBC URL
   * @param username SQL username
   * @param password SQL password
   */
  public ProxySqlClient(String jdbcUrl, String username, String password) {
    this.jdbcUrl = jdbcUrl;
    this.username = username;
    this.password = password;
  }

  /**
   * Ensures heartbeat table exists.
   */
  public void initializeSchema() throws SQLException {
    String sql = "CREATE TABLE IF NOT EXISTS dd_proxy_presence ("
        + "proxy_id VARCHAR(128) PRIMARY KEY,"
        + "platform VARCHAR(32) NOT NULL,"
        + "last_seen_epoch_ms BIGINT NOT NULL"
        + ")";
    try (Connection connection = openConnection();
         Statement statement = connection.createStatement()) {
      statement.executeUpdate(sql);
    }
  }

  /**
   * Writes one heartbeat row.
   *
   * @param proxyId logical proxy ID
   * @param platform proxy platform label
   * @param epochMillis heartbeat time
   */
  public void upsertHeartbeat(String proxyId, String platform, long epochMillis) throws SQLException {
    String updateSql = "UPDATE dd_proxy_presence SET platform=?, last_seen_epoch_ms=? WHERE proxy_id=?";
    try (Connection connection = openConnection();
         PreparedStatement update = connection.prepareStatement(updateSql)) {
      update.setString(1, platform);
      update.setLong(2, epochMillis);
      update.setString(3, proxyId);
      int changed = update.executeUpdate();
      if (changed == 0) {
        String insertSql = "INSERT INTO dd_proxy_presence (proxy_id, platform, last_seen_epoch_ms) VALUES (?, ?, ?)";
        try (PreparedStatement insert = connection.prepareStatement(insertSql)) {
          insert.setString(1, proxyId);
          insert.setString(2, platform);
          insert.setLong(3, epochMillis);
          insert.executeUpdate();
        }
      }
    }
  }

  private Connection openConnection() throws SQLException {
    if (username == null || username.isBlank()) {
      return DriverManager.getConnection(jdbcUrl);
    }
    return DriverManager.getConnection(jdbcUrl, username, password == null ? "" : password);
  }
}
