package me.ryanhamshire.GriefPrevention;

/**
 * Minimal stand-in for GriefPrevention's ClaimPermission enum so reflection
 * lookups ({@code Class.forName} + {@code Enum.valueOf}) resolve during tests.
 */
public enum ClaimPermission {
  Build,
  Access,
  Inventory,
  Manage,
  Login
}
