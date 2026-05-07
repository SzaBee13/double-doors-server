package me.szabee.doubledoors.version;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import java.util.Optional;

/**
 * Version bridge for Minecraft 1.21.x API.
 */
public final class VersionBridgeImpl implements VersionBridge {
  /**
   * Gets the server API version string for diagnostics.
   *
   * @return server api version or "unknown" when Bukkit version is blank
   */
  @Override
  public String getServerApiVersion() {
    String version = Bukkit.getBukkitVersion();
    return version == null || version.isBlank() ? "unknown" : version;
  }

  /**
   * Creates a listener instance for any version-specific event handling.
   *
   * @return an empty optional as no version-specific listener is needed for 1.21.x
   */
  @Override
  public Optional<Listener> createVersionListener() {
    return Optional.empty();
  }
}
