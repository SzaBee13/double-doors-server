package me.szabee.doubledoors.version;

import java.util.Optional;
import org.bukkit.event.Listener;

/**
 * Version-specific bridge for Bukkit API differences.
 */
public interface VersionBridge {
  /**
   * Gets the server API version string for diagnostics.
   *
   * @return server api version or "unknown"
   */
  String getServerApiVersion();

  /**
   * Creates a listener instance for any version-specific event handling.
   *
   * @return an optional listener
   */
  Optional<Listener> createVersionListener();
}
