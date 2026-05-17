package me.szabee.doubledoors.storage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;

final class SharedSqlStorageTest {

  @Test
  void testRecentProxyGeyserBridgeRequiresBridgeFlag() throws SQLException {
    String jdbcUrl = "jdbc:sqlite:target/test-shared-proxy-" + UUID.randomUUID() + ".db";
    SharedSqlStorage storage = new SharedSqlStorage(Logger.getLogger("test"), jdbcUrl, "", "");
    storage.initializeSchema();

    long now = System.currentTimeMillis();
    insertProxyHeartbeat(jdbcUrl, "plain-proxy", now, false, false);
    assertFalse(storage.hasRecentProxyGeyserBridge(60_000L));

    insertProxyHeartbeat(jdbcUrl, "geyser-proxy", now, true, false);
    assertTrue(storage.hasRecentProxyGeyserBridge(60_000L));
  }

  @Test
  void testStaleProxyGeyserBridgeIsIgnored() throws SQLException {
    String jdbcUrl = "jdbc:sqlite:target/test-stale-proxy-" + UUID.randomUUID() + ".db";
    SharedSqlStorage storage = new SharedSqlStorage(Logger.getLogger("test"), jdbcUrl, "", "");
    storage.initializeSchema();

    insertProxyHeartbeat(jdbcUrl, "stale-proxy", System.currentTimeMillis() - 120_000L, true, true);

    assertFalse(storage.hasRecentProxyGeyserBridge(30_000L));
  }

  private static void insertProxyHeartbeat(String jdbcUrl, String proxyId, long lastSeenMillis, boolean hasGeyser,
    boolean hasFloodgate) throws SQLException {
    String sql = "INSERT INTO dd_proxy_presence "
      + "(proxy_id, platform, last_seen_epoch_ms, has_geyser, has_floodgate) VALUES (?, 'velocity', ?, ?, ?)";
    try (Connection connection = DriverManager.getConnection(jdbcUrl);
      PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, proxyId);
      statement.setLong(2, lastSeenMillis);
      statement.setBoolean(3, hasGeyser);
      statement.setBoolean(4, hasFloodgate);
      statement.executeUpdate();
    }
  }
}
