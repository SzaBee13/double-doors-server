package me.szabee.doubledoors.bukkit.util;

import dev.faststats.ErrorTracker;
import dev.faststats.bukkit.BukkitContext;
import dev.faststats.bukkit.BukkitMetrics;
import dev.faststats.data.Metric;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import me.szabee.doubledoors.bukkit.DoubleDoors;
import me.szabee.doubledoors.bukkit.config.PluginConfig;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Manages FastStats anonymous metrics for DoubleDoors.
 *
 * <p>FastStats invokes metric suppliers on an asynchronous scheduler
 * ({@code BukkitContext.scheduleAtFixedRate} uses the async scheduler or
 * {@code runTaskTimerAsynchronously}). Bukkit state therefore must never be
 * touched inside a supplier: this manager maintains main-thread-computed
 * snapshots of everything that requires live server access, and suppliers only
 * read those snapshot fields.</p>
 */
public final class FastStatsManager {

  private static final String TOKEN_PATTERN = "[a-z0-9]{32}";
  private static final String PROJECT_TOKEN =
    "883c734d766f7078fa4525e9c573c8af";

  /**
   * Period, in server ticks, at which the main-thread snapshots consumed by the
   * async collectors are rebuilt. FastStats submits every 30 minutes, so one
   * minute of staleness is acceptable.
   */
  private static final long SNAPSHOT_REFRESH_PERIOD_TICKS = 1200L;

  private final JavaPlugin plugin;
  private volatile BukkitContext metricsContext;
  private volatile BukkitTask snapshotTask;
  private volatile PluginConfig activeConfig;
  private volatile String[] perPlayerLocaleSnapshot = new String[0];
  private volatile String updateCheckerSnapshot = "on";

  /**
   * Creates a new FastStats manager bound to the given plugin.
   *
   * @param plugin the Bukkit plugin instance used for logging and server access
   */
  public FastStatsManager(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  /**
   * Starts or restarts FastStats. Safe to call multiple times.
   *
   * <p>Must be invoked on the main server thread because it reads live plugin
   * state to seed the snapshots used by the async collectors.</p>
   *
   * @param config the current plugin configuration
   */
  public void restart(PluginConfig config) {
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

    activeConfig = config;
    refreshPerPlayerLocaleSnapshot(config);
    updateCheckerSnapshot = resolveUpdateChecker(
      plugin.getServer().getPluginManager()
    );

    try {
      BukkitContext context = new BukkitContext.Factory(plugin, token)
        .metrics(factory -> {
          BukkitMetrics.Factory bFactory = (BukkitMetrics.Factory) factory;
          addMetrics(bFactory, config);
          return bFactory.create();
        })
        .errorTrackerService(
          ErrorTracker.contextAware(getClass().getClassLoader())
        )
        .create();
      context.getLoggerFactory().setDebug(config.isDebug());
      context.ready();
      metricsContext = context;
      startSnapshotRefreshTask();
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

  /** Shuts down the metrics context and snapshot refresh task if running. */
  public void shutdown() {
    BukkitTask task = snapshotTask;
    if (task != null) {
      task.cancel();
      snapshotTask = null;
    }
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
    PluginConfig config
  ) {
    factory
      .addMetric(Metric.string("server_language", config::getLanguage))
      .addMetric(
        Metric.string(
          "data_storage_type",
          () -> resolveDataStorageType(config)
        )
      )
      .addMetric(Metric.bool("auto_close_enabled", config::isEnableAutoClose))
      .addMetric(Metric.bool("knocking_enabled", config::isEnableKnockFeature))
      .addMetric(
        Metric.string(
          "update_checker",
          this::currentUpdateChecker
        )
      )
      .addMetric(Metric.bool("debug_enabled", config::isDebug))
      .addMetric(
        Metric.number(
          "recursive_opening",
          () -> config.isEnableRecursiveOpening()
            ? config.getRecursiveOpeningMaxBlocksDistance()
            : 0
        )
      )
      .addMetric(
        Metric.stringArray(
          "per_player_locales",
          this::currentPerPlayerLocales
        )
      );
  }

  /**
   * Rebuilds the per-player locale snapshot from live server state.
   *
   * <p>Runs on the main thread only; the async collectors read the resulting
   * array through {@link #currentPerPlayerLocales()}.</p>
   *
   * @param config the current plugin configuration
   */
  void refreshPerPlayerLocaleSnapshot(PluginConfig config) {
    if (!config.isPerPlayerLocaleEnabled()) {
      perPlayerLocaleSnapshot = new String[0];
      return;
    }
    DoubleDoors dd = (DoubleDoors) plugin;
    if (dd.getPlayerPreferences() == null) {
      perPlayerLocaleSnapshot = new String[0];
      return;
    }
    List<String> locales = new ArrayList<>();
    String fallback = config.getLanguage();
    for (Player player : plugin.getServer().getOnlinePlayers()) {
      String locale = dd.getPlayerPreferences().getLocale(player.getUniqueId());
      locales.add(locale != null && !locale.isBlank() ? locale : fallback);
    }
    perPlayerLocaleSnapshot = locales.toArray(new String[0]);
  }

  /**
   * Returns the latest main-thread-computed per-player locale snapshot.
   *
   * <p>Safe to call from any thread.</p>
   *
   * @return the snapshot array; empty when per-player locales are disabled
   */
  String[] currentPerPlayerLocales() {
    return perPlayerLocaleSnapshot;
  }

  /**
   * Returns the cached update-checker resolution.
   *
   * @return the last value computed on the main thread by {@link #restart}
   */
  String currentUpdateChecker() {
    return updateCheckerSnapshot;
  }

  private void startSnapshotRefreshTask() {
    snapshotTask =
      plugin
        .getServer()
        .getScheduler()
        .runTaskTimer(
          plugin,
          () -> refreshPerPlayerLocaleSnapshot(activeConfig),
          SNAPSHOT_REFRESH_PERIOD_TICKS,
          SNAPSHOT_REFRESH_PERIOD_TICKS
        );
  }

  static String resolveDataStorageType(PluginConfig config) {
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

  static String resolveUpdateChecker(PluginManager pluginManager) {
    for (org.bukkit.plugin.Plugin p : pluginManager.getPlugins()) {
      if (!p.isEnabled()) {
        continue;
      }
      String name = p.getName();
      if (
        name.equalsIgnoreCase("PluginUpdater") ||
        name.equalsIgnoreCase("PluginUpdaterPlugin")
      ) {
        return "pluginupdater";
      }
    }
    return "on";
  }

  static String normalizeToken(String rawToken) {
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
