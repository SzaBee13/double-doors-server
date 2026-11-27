package me.szabee.doubledoors.bukkit.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Level;
import me.szabee.doubledoors.bukkit.config.PluginConfig;
import me.szabee.doubledoors.util.TaskToken;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.lushplugins.pluginupdater.api.updater.PluginData;
import org.lushplugins.pluginupdater.api.updater.Updater;

/**
 * Manages the built-in PluginUpdater update checker for DoubleDoors.
 */
public final class PluginUpdaterManager {

  private static final String MODRINTH_PROJECT_ID = "Fdj5mcgC";
  private static final String NOTIFY_PERMISSION = "doubledoors.update.notify";
  private static final String DELEGATED_LOG =
    "PluginUpdater plugin detected; built-in DoubleDoors update checks are disabled to avoid duplicate notifications.";

  private final JavaPlugin plugin;
  private volatile Updater updater;
  private volatile TaskToken updaterCheckTask;

  public PluginUpdaterManager(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  /** Initializes the updater based on the given config. Safe to call multiple times. */
  public void initialize(PluginConfig config) {
    disable();

    if (!config.isUpdateCheckerEnabled()) {
      plugin.getLogger().info(
        "Built-in updater checks are disabled by config (updateChecker.enabled=false)."
      );
      return;
    }

    if (isPluginUpdaterPluginPresent()) {
      plugin.getLogger().info(DELEGATED_LOG);
      plugin.getLogger().info(
        "Ensure the external PluginUpdater plugin is configured to include DoubleDoors update checks."
      );
      return;
    }

    try {
      Updater.Builder builder = Updater.builder(plugin);
      injectPluginData(builder);
      updater = builder
        .modrinth(MODRINTH_PROJECT_ID)
        .notify(config.isUpdateCheckerNotify())
        .notificationPermission(NOTIFY_PERMISSION)
        .build();
      scheduleChecks(config);
      plugin.getLogger().info("Built-in updater checks are enabled for DoubleDoors.");
    } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
      plugin.getLogger().log(
        Level.WARNING,
        "Plugin updater could not be initialized; continuing without update checks.",
        e
      );
    }
  }

  /** Disables the updater and cancels any scheduled checks. */
  public void disable() {
    TaskToken localTask = updaterCheckTask;
    updaterCheckTask = null;
    if (localTask != null) {
      localTask.cancel();
    }

    Updater localUpdater = updater;
    if (localUpdater == null) {
      return;
    }

    PluginData pluginData = localUpdater.getPluginData();
    if (pluginData != null) {
      pluginData.setEnabled(false);
    }
    updater = null;
  }

  private boolean isPluginUpdaterPluginPresent() {
    PluginManager pm = plugin.getServer().getPluginManager();
    for (org.bukkit.plugin.Plugin p : pm.getPlugins()) {
      if (!p.isEnabled()) {
        continue;
      }
      String name = p.getName();
      if (name.equalsIgnoreCase("PluginUpdater") || name.equalsIgnoreCase("PluginUpdaterPlugin")) {
        return true;
      }
    }
    return false;
  }

  private void injectPluginData(Updater.Builder builder)
    throws ReflectiveOperationException {
    Objects.requireNonNull(builder, "builder");
    PluginData pluginData = PluginData.builder(plugin)
      .platformData(new ArrayList<>())
      .build();

    // Upstream API request tracker (open when authenticated):
    // https://github.com/OakLoaf/PluginUpdater/issues/new?title=Expose%20Builder%20API%20for%20platformData%20injection
    Field pluginDataField = Updater.Builder.class.getDeclaredField("pluginData");
    pluginDataField.setAccessible(true);
    if (!pluginDataField.getType().isAssignableFrom(pluginData.getClass())) {
      throw new ReflectiveOperationException(
        "Unexpected Updater.Builder#pluginData type: " +
          pluginDataField.getType().getName()
      );
    }
    pluginDataField.set(builder, pluginData);
  }

  private void scheduleChecks(PluginConfig config) {
    if (updater == null) {
      return;
    }
    long checkFrequencySeconds = config.getUpdateCheckerScheduleSeconds();
    if (checkFrequencySeconds <= 0L) {
      return;
    }
    long periodTicks = checkFrequencySeconds * 20L;
    updaterCheckTask = SchedulerBridge.runTimerAsync(
      plugin,
      0L,
      periodTicks,
      () -> {
        Updater localUpdater = updater;
        if (localUpdater != null) {
          localUpdater.checkForUpdate();
        }
      }
    );
  }
}
