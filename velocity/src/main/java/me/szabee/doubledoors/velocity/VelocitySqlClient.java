package me.szabee.doubledoors.velocity;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * SQL client used by the proxy module for heartbeat writes.
 * Uses HikariCP for efficient connection pooling.
 */
public final class VelocitySqlClient {

  private static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";
  private static final String SQLITE_DRIVER = "org.sqlite.JDBC";
  private static final String SQLITE_UPSERT_SQL = "INSERT INTO dd_proxy_presence "
    + "(proxy_id, platform, last_seen_epoch_ms, has_geyser, has_floodgate) VALUES (?, ?, ?, ?, ?) "
    + "ON CONFLICT(proxy_id) DO UPDATE SET platform=excluded.platform, "
    + "last_seen_epoch_ms=excluded.last_seen_epoch_ms, has_geyser=excluded.has_geyser, has_floodgate=excluded.has_floodgate";
  private static final String MYSQL_UPSERT_SQL = "INSERT INTO dd_proxy_presence "
    + "(proxy_id, platform, last_seen_epoch_ms, has_geyser, has_floodgate) VALUES (?, ?, ?, ?, ?) "
    + "ON DUPLICATE KEY UPDATE platform=VALUES(platform), last_seen_epoch_ms=VALUES(last_seen_epoch_ms), "
    + "has_geyser=VALUES(has_geyser), has_floodgate=VALUES(has_floodgate)";

  private final HikariDataSource dataSource;
  private final String upsertSql;

  /**
   * Creates a SQL client with HikariCP connection pooling.
   *
   * @param jdbcUrl JDBC URL
   * @param username SQL username
   * @param password SQL password
   */
  public VelocitySqlClient(String jdbcUrl, String username, String password) {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(jdbcUrl);
    String driverClassName = detectDriverClassName(jdbcUrl);
    if (driverClassName != null) {
      ensureDriverLoaded(driverClassName);
      config.setDriverClassName(driverClassName);
    }
    if (username != null && !username.isBlank()) {
      config.setUsername(username);
      config.setPassword(password == null ? "" : password);
    }
    config.setMaximumPoolSize(5);
    config.setMinimumIdle(1);
    config.setConnectionTimeout(10_000);
    config.setIdleTimeout(600_000);
    config.setMaxLifetime(1_800_000);
    ensureSqliteParentDirectoryExists(jdbcUrl, driverClassName);
    this.dataSource = new HikariDataSource(config);
    this.upsertSql = SQLITE_DRIVER.equals(driverClassName) ? SQLITE_UPSERT_SQL : MYSQL_UPSERT_SQL;
  }

  private static void ensureSqliteParentDirectoryExists(String jdbcUrl, String driverClassName) {
    if (!SQLITE_DRIVER.equals(driverClassName) || jdbcUrl == null) {
      return;
    }

    String normalizedUrl = jdbcUrl.toLowerCase();
    if (!normalizedUrl.startsWith("jdbc:sqlite:")) {
      return;
    }

    String path = jdbcUrl.substring("jdbc:sqlite:".length());
    if (path.isBlank() || path.startsWith(":memory:")) {
      return;
    }

    Path dbPath = Paths.get(path);
    Path parent = dbPath.getParent();
    if (parent == null) {
      return;
    }

    try {
      Files.createDirectories(parent);
    } catch (Exception exception) {
      throw new IllegalStateException("Could not create SQLite database directory: " + parent, exception);
    }
  }

  private static String detectDriverClassName(String jdbcUrl) {
    if (jdbcUrl == null || !jdbcUrl.regionMatches(true, 0, "jdbc:", 0, 5)) {
      return null;
    }
    int databaseTypeEnd = jdbcUrl.indexOf(':', 5);
    if (databaseTypeEnd < 0) {
      return null;
    }
    String databaseType = jdbcUrl.substring(5, databaseTypeEnd).toLowerCase();
    return switch (databaseType) {
      case "mysql" -> MYSQL_DRIVER;
      case "sqlite" -> SQLITE_DRIVER;
      default -> null;
    };
  }

  private static void ensureDriverLoaded(String driverClassName) {
    if (driverClassName == null) {
      return;
    }
    // JDBC 4+ drivers are typically auto-registered via ServiceLoader;
    // this explicit load is kept as a fast-fail diagnostic for environments where that is not true.
    try {
      Class.forName(driverClassName);
    } catch (ClassNotFoundException exception) {
      throw new IllegalStateException("JDBC driver class not found: " + driverClassName, exception);
    }
  }

  /**
   * Ensures heartbeat table exists.
   */
  public void initializeSchema() throws SQLException {
    String sql = "CREATE TABLE IF NOT EXISTS dd_proxy_presence ("
      + "proxy_id VARCHAR(128) PRIMARY KEY,"
      + "platform VARCHAR(32) NOT NULL,"
      + "last_seen_epoch_ms BIGINT NOT NULL,"
      + "has_geyser BOOLEAN NOT NULL DEFAULT FALSE,"
      + "has_floodgate BOOLEAN NOT NULL DEFAULT FALSE"
      + ")";
    try (Connection connection = dataSource.getConnection();
      Statement statement = connection.createStatement()) {
      statement.executeUpdate(sql);
      executeAlterAddColumnIfAbsent(connection,
        "ALTER TABLE dd_proxy_presence ADD COLUMN has_geyser BOOLEAN NOT NULL DEFAULT FALSE");
      executeAlterAddColumnIfAbsent(connection,
        "ALTER TABLE dd_proxy_presence ADD COLUMN has_floodgate BOOLEAN NOT NULL DEFAULT FALSE");
    }
  }

  /**
   * Writes one heartbeat row.
   *
   * @param proxyId logical proxy ID
   * @param platform proxy platform label
   * @param epochMillis heartbeat time
   * @param hasGeyser true when the proxy has Geyser installed
   * @param hasFloodgate true when the proxy has Floodgate installed
   */
  public void upsertHeartbeat(String proxyId, String platform, long epochMillis, boolean hasGeyser,
    boolean hasFloodgate) throws SQLException {
    try (Connection connection = dataSource.getConnection();
      PreparedStatement upsert = connection.prepareStatement(upsertSql)) {
      upsert.setString(1, proxyId);
      upsert.setString(2, platform);
      upsert.setLong(3, epochMillis);
      upsert.setBoolean(4, hasGeyser);
      upsert.setBoolean(5, hasFloodgate);
      upsert.executeUpdate();
    }
  }

  private static void executeAlterAddColumnIfAbsent(Connection connection, String sql) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.executeUpdate(sql);
    } catch (SQLException e) {
      String message = e.getMessage();
      String normalizedMessage = message == null ? "" : message.toLowerCase();
      if (e.getErrorCode() == 1060 || normalizedMessage.contains("duplicate column")
        || normalizedMessage.contains("already exists")) {
        return;
      }
      throw e;
    }
  }

  /**
   * Closes the connection pool. Call this on proxy shutdown.
   */
  public void close() {
    if (dataSource != null && !dataSource.isClosed()) {
      dataSource.close();
    }
  }
}
