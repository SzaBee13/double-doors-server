package me.szabee.doubledoors.util;

/**
 * Shared scheduler abstraction.
 */
public interface SchedulerBridge {
  /**
   * Schedules a runnable later.
   *
   * @param runnable task to run
   * @param delayTicks delay in ticks
   * @return task token
   */
  TaskToken runLater(Runnable runnable, long delayTicks);
}
