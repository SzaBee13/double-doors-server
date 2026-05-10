package me.szabee.doubledoors.proxy;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQL client used by the proxy module for heartbeat writes.
 * Uses HikariCP for efficient connection pooling.
 */
public final class ProxySqlClient {

  private static final String MYSQL_DRIVER = "com.mysql.cj.jdbc.Driver";
  private static final String SQLITE_DRIVER = "org.sqlite.JDBC";
  private static final String SQLITE_UPSERT_SQL = "INSERT INTO dd_proxy_presence (proxy_id, platform, last_seen_epoch_ms) VALUES (?, ?, ?) "
    + "ON CONFLICT(proxy_id) DO UPDATE SET platform=excluded.platform, last_seen_epoch_ms=excluded.last_seen_epoch_ms";
  private static final String MYSQL_UPSERT_SQL = "INSERT INTO dd_proxy_presence (proxy_id, platform, last_seen_epoch_ms) VALUES (?, ?, ?) "
    + "ON DUPLICATE KEY UPDATE platform=VALUES(platform), last_seen_epoch_ms=VALUES(last_seen_epoch_ms)";

  private final HikariDataSource dataSource;
  private final String upsertSql;

  /**
   * Creates a SQL client with HikariCP connection pooling.
   *
   * @param jdbcUrl JDBC URL
   * @param username SQL username
   * @param password SQL password
   */
  public ProxySqlClient(String jdbcUrl, String username, String password) {
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
    this.dataSource = new HikariDataSource(config);
    this.upsertSql = SQLITE_DRIVER.equals(driverClassName) ? SQLITE_UPSERT_SQL : MYSQL_UPSERT_SQL;
  }

  private static String detectDriverClassName(String jdbcUrl) {
  if (jdbcUrl == null) {
    return null;
  }
  if (jdbcUrl.startsWith("jdbc:mysql:")) {
    return MYSQL_DRIVER;
  }
  if (jdbcUrl.startsWith("jdbc:mariadb:")) {
    return MYSQL_DRIVER;
  }
  if (jdbcUrl.startsWith("jdbc:sqlite:")) {
    return SQLITE_DRIVER;
  }
  return null;
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
    + "last_seen_epoch_ms BIGINT NOT NULL"
    + ")";
  try (Connection connection = dataSource.getConnection();
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
    try (Connection connection = dataSource.getConnection();
      PreparedStatement upsert = connection.prepareStatement(upsertSql)) {
      upsert.setString(1, proxyId);
      upsert.setString(2, platform);
      upsert.setLong(3, epochMillis);
      upsert.executeUpdate();
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
