package me.szabee.doubledoors.bukkit.util;

import org.bukkit.plugin.Plugin;

/**
 * Abstract stand-in for the GriefPrevention plugin. Mockito generates a
 * subclass of this type, which therefore inherits the public {@code dataStore}
 * field that {@link ProtectionCompat} resolves reflectively.
 */
abstract class FakeGriefPreventionBase implements Plugin {

  /** Mirrors GriefPrevention's public {@code dataStore} field. */
  public Object dataStore;
}
