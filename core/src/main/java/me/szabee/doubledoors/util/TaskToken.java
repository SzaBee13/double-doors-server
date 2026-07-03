package me.szabee.doubledoors.util;

/**
 * Handle for a scheduled task.
 */
public interface TaskToken {
  /** Cancels the task. */
  void cancel();
}
