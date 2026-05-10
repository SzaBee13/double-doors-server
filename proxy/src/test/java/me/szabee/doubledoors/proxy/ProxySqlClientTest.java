package me.szabee.doubledoors.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import java.util.UUID;

final class ProxySqlClientTest {

  @Test
  void testSQLiteHeartbeat() throws SQLException {
    String dbPath = "target/test-heartbeat-" + UUID.randomUUID() + ".db";
    String jdbcUrl = "jdbc:sqlite:" + dbPath;
    ProxySqlClient client = new ProxySqlClient(jdbcUrl, "", "");
    
    try {
      client.initializeSchema();
      
      long now = System.currentTimeMillis();
      client.upsertHeartbeat("proxy1", "velocity", now);
      client.upsertHeartbeat("proxy1", "velocity", now + 1000);

      try (Connection connection = DriverManager.getConnection(jdbcUrl);
        PreparedStatement statement = connection.prepareStatement(
          "SELECT proxy_id, platform, last_seen_epoch_ms, COUNT(*) OVER() AS row_count FROM dd_proxy_presence WHERE proxy_id = ?")) {
        statement.setString(1, "proxy1");
        try (ResultSet resultSet = statement.executeQuery()) {
          assertTrue(resultSet.next());
          assertEquals("proxy1", resultSet.getString("proxy_id"));
          assertEquals("velocity", resultSet.getString("platform"));
          assertEquals(now + 1000, resultSet.getLong("last_seen_epoch_ms"));
          assertEquals(1, resultSet.getInt("row_count"));
        }
      }
    } finally {
      client.close();
      // Cleanup database file handled by being in target/
    }
  }
}
