package me.szabee.doubledoors.bukkit.util;

import dev.faststats.ErrorTracker;
import dev.faststats.bukkit.BukkitContext;
import dev.faststats.bukkit.BukkitMetrics;
import dev.faststats.data.Metric;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.logging.Level;
import me.szabee.doubledoors.bukkit.config.PluginConfig;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Manages FastStats anonymous metrics for DoubleDoors.
 */
public final class FastStatsManager {

  private static final String TOKEN_PATTERN = "[a-z0-9]{32}";
  private static final String PROJECT_TOKEN =
    "883c734d766f7078fa4525e9c573c8af";

  private final JavaPlugin plugin;
  private final long startedAtNanos = System.nanoTime();
  private volatile BukkitContext metricsContext;

  /**
   * Creates a new FastStats manager bound to the given plugin.
   *
   * @param plugin the Bukkit plugin instance used for logging and server access
   */
  public FastStatsManager(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  /** Starts or restarts FastStats. Safe to call multiple times. */
  public void restart(
    PluginConfig config,
    BooleanSupplier geyserBridgeAvailable
  ) {
    shutdown();
    if (!config.isEnableAnonymousTracking()) {
      plugin.getLogger().info("Anonymous tracking is disabled by config.");
      return;
    }

    String token = normalizeToken(PROJECT_TOKEN);
    if (token == null) {
      plugin
        .getLogger()
        .warning(
          "Anonymous tracking is enabled, but the built-in FastStats token is invalid; metrics are disabled."
        );
      return;
    }

    try {
      BukkitContext context = new BukkitContext.Factory(plugin, token)
        .metrics(factory -> {
          BukkitMetrics.Factory bFactory = (BukkitMetrics.Factory) factory;
          addMetrics(bFactory, config, geyserBridgeAvailable);
          return bFactory.create();
        })
        .errorTrackerService(
          ErrorTracker.contextAware(getClass().getClassLoader())
        )
        .create();
      context.getLoggerFactory().setDebug(config.isDebug());
      context.ready();
      metricsContext = context;
    } catch (RuntimeException e) {
      plugin
        .getLogger()
        .log(
          Level.WARNING,
          "FastStats could not be initialized; continuing without metrics.",
          e
        );
    }
  }

  /** Shuts down the metrics context if running. */
  public void shutdown() {
    BukkitContext ctx = metricsContext;
    if (ctx != null) {
      try {
        ctx.shutdown();
      } catch (RuntimeException e) {
        plugin
          .getLogger()
          .log(Level.WARNING, "FastStats could not be shut down cleanly.", e);
      } finally {
        metricsContext = null;
      }
    }
  }

  private void addMetrics(
    BukkitMetrics.Factory factory,
    PluginConfig config,
    BooleanSupplier geyserBridgeAvailable
  ) {
    factory
      .addMetric(Metric.string("server_language", config::getLanguage))
      .addMetric(
        Metric.string(
          "data_storage_type",
          () -> resolveDataStorageType(config)
        )
      )
      .addMetric(
        Metric.number("server_max_players", plugin.getServer()::getMaxPlayers)
      )
      .addMetric(
        Metric.number(
          "plugin_uptime_minutes",
          () -> (System.nanoTime() - startedAtNanos) / 60_000_000_000L
        )
      )
      .addMetric(Metric.bool("auto_close_enabled", config::isEnableAutoClose))
      .addMetric(Metric.bool("knocking_enabled", config::isEnableKnockFeature))
      .addMetric(
        Metric.bool("update_checker_enabled", config::isUpdateCheckerEnabled)
      )
      .addMetric(Metric.bool("debug_enabled", config::isDebug))
      .addMetric(
        Metric.bool(
          "recursive_opening_enabled",
          config::isEnableRecursiveOpening
        )
      )
      .addMetric(
        Metric.bool("geyser_detected", geyserBridgeAvailable::getAsBoolean)
      )
      .addMetric(
        Metric.bool("worldguard_detected", () ->
          plugin.getServer().getPluginManager().isPluginEnabled("WorldGuard")
        )
      )
      .addMetric(
        Metric.bool("griefprevention_detected", () ->
          plugin
            .getServer()
            .getPluginManager()
            .isPluginEnabled("GriefPrevention")
        )
      );
  }

  private static String resolveDataStorageType(PluginConfig config) {
    if (!config.isSqlEnabled()) {
      return "yml";
    }
    String url = config.getSqlJdbcUrl();
    if (url == null) {
      return "yml";
    }
    String lower = url.toLowerCase(Locale.ROOT);
    if (lower.startsWith("jdbc:sqlite:")) {
      return "sqlite";
    }
    if (lower.startsWith("jdbc:mysql:")) {
      return "mysql";
    }
    return "unknown";
  }

  private static String normalizeToken(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      return null;
    }
    String normalized = rawToken
      .trim()
      .toLowerCase(Locale.ROOT)
      .replace("-", "");
    if (!normalized.matches(TOKEN_PATTERN)) {
      return null;
    }
    return normalized;
  }
}
