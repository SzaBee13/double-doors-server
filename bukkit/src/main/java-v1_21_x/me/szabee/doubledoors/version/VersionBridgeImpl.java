package me.szabee.doubledoors.version;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

/**
 * Version bridge for Minecraft 1.21.x API.
 */
public final class VersionBridgeImpl implements VersionBridge {
  @Override
  public String getServerApiVersion() {
    String version = Bukkit.getBukkitVersion();
    return version == null || version.isBlank() ? "unknown" : version;
  }

  @Override
  public Listener createVersionListener() {
    return null;
  }
}
