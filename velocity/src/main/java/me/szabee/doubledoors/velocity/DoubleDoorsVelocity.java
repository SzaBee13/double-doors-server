package me.szabee.doubledoors.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.faststats.velocity.VelocityContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

/**
 * Velocity-side component that reports proxy heartbeat into shared SQL storage.
 */
@Plugin(
  id = "doubledoors-velocity",
  name = "DoubleDoorsVelocity",
  version = "${project.version}",
  description = "Proxy companion plugin for DoubleDoors shared SQL detection",
  authors = { "SzaBee13" }
)
public final class DoubleDoorsVelocity {

  private static final String FASTSTATS_TOKEN_PATTERN = "[a-z0-9]{32}";
  private static final String FASTSTATS_PROJECT_TOKEN =
    "883c734d766f7078fa4525e9c573c8af"; // This should be public since it only identifies the project, not individual servers.

  private final ProxyServer proxyServer;
  private final Logger logger;
  private final Path dataDirectory;

  private VelocitySqlClient sqlClient;
  private String proxyId;
  private boolean geyserPresent;
  private boolean floodgatePresent;
  private boolean heartbeatEnabled;
  private VelocityContext metricsContext;

  /**
   * Creates the Velocity plugin instance.
   *
   * @param proxyServer Velocity proxy server
   * @param logger plugin logger
   * @param dataDirectory plugin data directory
   */
  @Inject
  public DoubleDoorsVelocity(
    ProxyServer proxyServer,
    Logger logger,
    @DataDirectory Path dataDirectory
  ) {
    this.proxyServer = proxyServer;
    this.logger = logger;
    this.dataDirectory = dataDirectory;
  }

  /**
   * Initializes SQL heartbeat reporting once Velocity starts.
   *
   * @param event initialize event
   */
  @Subscribe
  public void onProxyInitialize(ProxyInitializeEvent event) {
    Properties config = loadConfig();
    boolean anonymousTrackingEnabled = Boolean.parseBoolean(
      config.getProperty("enableAnonymousTracking", "true")
    );
    if (anonymousTrackingEnabled) {
      String token = normalizeFastStatsToken(FASTSTATS_PROJECT_TOKEN);
      if (token == null) {
        metricsContext = null;
        logger.warn(
          "DoubleDoorsVelocity anonymous tracking is enabled, but the built-in FastStats token is invalid;" +
            " metrics are disabled."
        );
      }

      if (token != null) {
        try {
          var pluginContainer = proxyServer
            .getPluginManager()
            .getPlugin("doubledoors-velocity")
            .orElseThrow(() ->
              new RuntimeException("Could not find own plugin container")
            );
          VelocityContext context = new VelocityContext.Factory(
            pluginContainer,
            proxyServer,
            logger,
            dataDirectory
          )
            .token(token)
            .metrics(factory -> factory.create())
            .create();
          context.ready();
          metricsContext = context;
        } catch (RuntimeException e) {
          metricsContext = null;
          logger.warn(
            "DoubleDoorsVelocity FastStats could not be initialized; continuing without metrics.",
            e
          );
        }
      }
    } else {
      metricsContext = null;
      logger.info(
        "DoubleDoorsVelocity anonymous tracking is disabled by config."
      );
    }

    boolean sqlEnabled = Boolean.parseBoolean(
      config.getProperty("sql.enabled", "false")
    );
    if (!sqlEnabled) {
      logger.info("DoubleDoorsVelocity SQL heartbeat is disabled by config.");
      return;
    }

    geyserPresent = isPluginPresent("geyser", "geyser-velocity");
    floodgatePresent = isPluginPresent("floodgate", "floodgate-velocity");
    if (!geyserPresent && !floodgatePresent) {
      logger.info(
        "DoubleDoorsVelocity did not detect Geyser/Floodgate on this proxy."
      );
      return;
    }

    String jdbcUrl = config
      .getProperty(
        "sql.jdbcUrl",
        "jdbc:sqlite:plugins/DoubleDoors/doubledoors.db"
      )
      .trim();
    String username = config.getProperty("sql.username", "").trim();
    String password = config.getProperty("sql.password", "");
    proxyId = config.getProperty("sql.proxyId", "velocity-main").trim();
    if (proxyId.isEmpty()) {
      proxyId = "velocity-main";
    }

    long heartbeatSeconds;
    try {
      heartbeatSeconds = Long.parseLong(
        config.getProperty("sql.heartbeatSeconds", "30")
      );
    } catch (NumberFormatException ignored) {
      heartbeatSeconds = 30L;
    }
    if (heartbeatSeconds < 5L) {
      heartbeatSeconds = 5L;
    }

    sqlClient = new VelocitySqlClient(jdbcUrl, username, password);
    long repeatSeconds = heartbeatSeconds;
    proxyServer
      .getScheduler()
      .buildTask(this, () -> {
        try {
          sqlClient.initializeSchema();
          writeHeartbeat();
          heartbeatEnabled = true;
          proxyServer
            .getScheduler()
            .buildTask(this, this::writeHeartbeat)
            .repeat(repeatSeconds, TimeUnit.SECONDS)
            .schedule();
          logger.info(
            "DoubleDoorsVelocity heartbeat enabled for proxyId='{}' every {}s.",
            proxyId,
            repeatSeconds
          );
        } catch (SQLException e) {
          logger.warn(
            "DoubleDoorsVelocity could not initialize SQL heartbeat: {}",
            e.getMessage()
          );
        }
      })
      .schedule();
  }

  /**
   * Writes a final heartbeat when the proxy shuts down and closes the connection pool.
   *
   * @param event shutdown event
   */
  @Subscribe
  public void onProxyShutdown(ProxyShutdownEvent event) {
    if (heartbeatEnabled && sqlClient != null) {
      writeHeartbeat();
    }
    if (sqlClient != null) {
      sqlClient.close();
      sqlClient = null;
    }

    if (metricsContext != null) {
      metricsContext.shutdown();
      metricsContext = null;
    }
  }

  private boolean isPluginPresent(String... ids) {
    for (String id : ids) {
      Optional<?> plugin = proxyServer.getPluginManager().getPlugin(id);
      if (plugin.isPresent()) {
        return true;
      }
    }
    return false;
  }

  private void writeHeartbeat() {
    if (sqlClient == null) {
      return;
    }
    try {
      sqlClient.upsertHeartbeat(
        proxyId,
        "velocity",
        System.currentTimeMillis(),
        geyserPresent,
        floodgatePresent
      );
    } catch (SQLException e) {
      logger.warn(
        "DoubleDoorsVelocity heartbeat write failed: {}",
        e.getMessage()
      );
    }
  }

  private String normalizeFastStatsToken(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      return null;
    }

    String normalized = rawToken.trim().toLowerCase().replace("-", "");
    if (!normalized.matches(FASTSTATS_TOKEN_PATTERN)) {
      return null;
    }
    return normalized;
  }

  private Properties loadConfig() {
    Properties properties = new Properties();
    try {
      Files.createDirectories(dataDirectory);
      Path configFile = dataDirectory.resolve("config.properties");
      if (Files.notExists(configFile)) {
        try (
          InputStream in = getClass()
            .getClassLoader()
            .getResourceAsStream("proxy-config.properties")
        ) {
          if (in != null) {
            try (OutputStream out = Files.newOutputStream(configFile)) {
              in.transferTo(out);
            }
          }
        }
      }

      if (Files.exists(configFile)) {
        try (InputStream in = Files.newInputStream(configFile)) {
          properties.load(in);
        }
      }
    } catch (IOException e) {
      logger.warn(
        "DoubleDoorsVelocity could not read config.properties: {}",
        e.getMessage()
      );
    }
    return properties;
  }
}
