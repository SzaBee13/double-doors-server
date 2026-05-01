package me.szabee.doubledoors.util;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.Plugin;

/**
 * Routes scheduled work through Folia-aware schedulers when available.
 */
public final class SchedulerBridge {
  private SchedulerBridge() {
  }

  public static void runNextTick(Plugin plugin, Runnable task) {
    if (runFoliaGlobal(plugin, task, 1L)) {
      return;
    }
    plugin.getServer().getScheduler().runTask(plugin, task);
  }

  public static void runLaterAtLocation(Plugin plugin, Location location, long delayTicks, Runnable task) {
    if (runFoliaRegion(plugin, location, delayTicks, task)) {
      return;
    }
    plugin.getServer().getScheduler().runTaskLater(plugin, task, delayTicks);
  }

  public static void runAsync(Plugin plugin, Runnable task) {
    if (runFoliaAsync(plugin, task)) {
      return;
    }
    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
  }

  public static TaskToken runTimerAsync(Plugin plugin, long delayTicks, long periodTicks, Runnable task) {
    TaskToken foliaTask = runFoliaTimerAsync(plugin, delayTicks, periodTicks, task);
    if (foliaTask != null) {
      return foliaTask;
    }
    BukkitTask bukkitTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, task, delayTicks, periodTicks);
    return bukkitTask::cancel;
  }

  private static boolean runFoliaGlobal(Plugin plugin, Runnable task, long delayTicks) {
    try {
      Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
      Method runDelayed = scheduler.getClass().getMethod("runDelayed", Plugin.class, Consumer.class, long.class);
      runDelayed.invoke(scheduler, plugin, consumer(task), delayTicks);
      return true;
    } catch (ReflectiveOperationException ignored) {
      return false;
    }
  }

  private static boolean runFoliaRegion(Plugin plugin, Location location, long delayTicks, Runnable task) {
    try {
      Object scheduler = Bukkit.class.getMethod("getRegionScheduler").invoke(null);
      Method runDelayed = scheduler.getClass().getMethod("runDelayed", Plugin.class, Location.class, Consumer.class, long.class);
      runDelayed.invoke(scheduler, plugin, location, consumer(task), delayTicks);
      return true;
    } catch (ReflectiveOperationException ignored) {
      return false;
    }
  }

  private static boolean runFoliaAsync(Plugin plugin, Runnable task) {
    try {
      Object scheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
      Method runNow = scheduler.getClass().getMethod("runNow", Plugin.class, Consumer.class);
      runNow.invoke(scheduler, plugin, consumer(task));
      return true;
    } catch (ReflectiveOperationException ignored) {
      return false;
    }
  }

  private static TaskToken runFoliaTimerAsync(Plugin plugin, long delayTicks, long periodTicks, Runnable task) {
    try {
      Object scheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
      Method runAtFixedRate = scheduler.getClass().getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class);
      Object scheduledTask = runAtFixedRate.invoke(scheduler, plugin, consumer(task), delayTicks * 50L, periodTicks * 50L, TimeUnit.MILLISECONDS);
      Method cancel = scheduledTask.getClass().getMethod("cancel");
      return () -> {
        try {
          cancel.invoke(scheduledTask);
        } catch (ReflectiveOperationException ignored) {
        }
      };
    } catch (ReflectiveOperationException ignored) {
      return null;
    }
  }

  private static Consumer<Object> consumer(Runnable task) {
    return ignored -> task.run();
  }
}
