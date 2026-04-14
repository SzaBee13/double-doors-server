package me.szabee.doubledoors.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import me.szabee.doubledoors.DoubleDoors;
import me.szabee.doubledoors.config.PluginConfig;

/**
 * Optional protection-plugin compatibility hooks.
 */

/**
 * Optional protection-plugin compatibility hooks.
 */
public final class ProtectionCompat {
  private ProtectionCompat() {
  }

  /**
   * Checks whether a player may toggle a linked block according to protection rules.
   *
   * <p>Currently integrates with GriefPrevention when installed. If integration cannot
   * be resolved, the check fails open to avoid breaking door interactions.</p>
   *
   * @param plugin the plugin instance
   * @param player the interacting player
   * @param linkedBlock the linked block to toggle
   * @return true if the linked interaction is allowed
   */
  public static boolean canOpenLinkedDoor(DoubleDoors plugin, Player player, Block linkedBlock) {
    return explainLinkedDoorDeniedReason(plugin, player, linkedBlock).isEmpty();
  }

  /**
   * Explains why a linked interaction is denied.
   *
   * @param plugin the plugin instance
   * @param player the interacting player
   * @param linkedBlock the linked block to toggle
   * @return empty string when allowed, otherwise a machine-readable deny reason
   */
  public static String explainLinkedDoorDeniedReason(DoubleDoors plugin, Player player, Block linkedBlock) {
    String locationFilterReason = evaluateLocationFilters(plugin.getPluginConfig(), linkedBlock);
    if (!locationFilterReason.isEmpty()) {
      return locationFilterReason;
    }

    Plugin griefPrevention = Bukkit.getPluginManager().getPlugin("GriefPrevention");
    if (griefPrevention != null && griefPrevention.isEnabled()) {
      try {
        Object dataStore = getDataStore(griefPrevention);
        if (dataStore != null) {
          Object claim = findClaimAt(dataStore, linkedBlock);
          if (claim != null) {
            Boolean allowBuildResult = tryAllowBuild(claim, player, linkedBlock.getType());
            if (allowBuildResult != null && !allowBuildResult) {
              return "griefprevention_build_denied";
            }

            Boolean checkPermissionResult = tryCheckPermission(claim, player);
            if (checkPermissionResult != null && !checkPermissionResult) {
              return "griefprevention_permission_denied";
            }
          }
        }
      } catch (ReflectiveOperationException ex) {
        plugin.getLogger().fine("GriefPrevention compatibility check skipped: %s".formatted(ex.getMessage()));
      }
    }

    String worldGuardReason = evaluateWorldGuard(plugin.getPluginConfig(), player, linkedBlock);
    if (!worldGuardReason.isEmpty()) {
      return worldGuardReason;
    }

    return "";
  }

  /**
   * Checks location-level filters that do not require a player context.
   *
   * @param plugin the plugin instance
   * @param block the block to check
   * @return true when allowed by configured location filters
   */
  public static boolean isLocationAllowed(DoubleDoors plugin, Block block) {
    return evaluateLocationFilters(plugin.getPluginConfig(), block).isEmpty();
  }

  private static Object getDataStore(Plugin griefPrevention) throws ReflectiveOperationException {
    Class<?> gpClass = griefPrevention.getClass();

    try {
      Field dataStoreField = gpClass.getField("dataStore");
      return dataStoreField.get(griefPrevention);
    } catch (NoSuchFieldException ignored) {
      Field dataStoreField = gpClass.getDeclaredField("dataStore");
      dataStoreField.setAccessible(true);
      return dataStoreField.get(griefPrevention);
    }
  }

  private static Object findClaimAt(Object dataStore, Block linkedBlock) throws ReflectiveOperationException {
    Method getClaimAt = null;
    for (Method method : dataStore.getClass().getMethods()) {
      if (!method.getName().equals("getClaimAt")) {
        continue;
      }
      Class<?>[] parameterTypes = method.getParameterTypes();
      if (parameterTypes.length >= 1
          && "org.bukkit.Location".equals(parameterTypes[0].getName())) {
        getClaimAt = method;
        break;
      }
    }

    if (getClaimAt == null) {
      return null;
    }

    Object[] args = new Object[getClaimAt.getParameterCount()];
    Class<?>[] parameterTypes = getClaimAt.getParameterTypes();
    args[0] = linkedBlock.getLocation();
    for (int i = 1; i < parameterTypes.length; i++) {
      if (parameterTypes[i] == boolean.class || parameterTypes[i] == Boolean.class) {
        args[i] = Boolean.TRUE;
      } else {
        args[i] = null;
      }
    }

    return getClaimAt.invoke(dataStore, args);
  }

  private static Boolean tryAllowBuild(Object claim, Player player, Material material)
      throws ReflectiveOperationException {
    for (Method method : claim.getClass().getMethods()) {
      if (!method.getName().equals("allowBuild")) {
        continue;
      }

      Class<?>[] parameterTypes = method.getParameterTypes();
      if (parameterTypes.length != 2) {
        continue;
      }
      if (!Player.class.isAssignableFrom(parameterTypes[0])) {
        continue;
      }
      if (!Material.class.isAssignableFrom(parameterTypes[1])) {
        continue;
      }

      Object result = method.invoke(claim, player, material);
      if (result == null) {
        return true;
      }
      if (result instanceof String message) {
        return message.isEmpty();
      }
      return false;
    }

    return null;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static Boolean tryCheckPermission(Object claim, Player player) throws ReflectiveOperationException {
    Class<?> claimPermissionClass;
    try {
      claimPermissionClass = Class.forName("me.ryanhamshire.GriefPrevention.ClaimPermission");
    } catch (ClassNotFoundException ex) {
      return null;
    }

    Object buildPermission = Enum.valueOf((Class<Enum>) claimPermissionClass, "Build");

    for (Method method : claim.getClass().getMethods()) {
      if (!method.getName().equals("checkPermission")) {
        continue;
      }

      Class<?>[] parameterTypes = method.getParameterTypes();
      if (parameterTypes.length < 2) {
        continue;
      }
      if (!Player.class.isAssignableFrom(parameterTypes[0])) {
        continue;
      }
      if (!parameterTypes[1].isAssignableFrom(claimPermissionClass)
          && !claimPermissionClass.isAssignableFrom(parameterTypes[1])) {
        continue;
      }

      Object[] args = new Object[parameterTypes.length];
      args[0] = player;
      args[1] = buildPermission;
      for (int i = 2; i < parameterTypes.length; i++) {
        args[i] = null;
      }

      Object result = method.invoke(claim, args);
      if (result == null) {
        return true;
      }
      if (result instanceof Boolean boolResult) {
        return boolResult;
      }
      if (result instanceof String message) {
        return message.isEmpty();
      }
      if (result instanceof Supplier<?> supplier) {
        Object supplied = supplier.get();
        return supplied == null || supplied.toString().isEmpty();
      }

      return false;
    }

    return null;
  }

  /**
   * Returns the GriefPrevention claim ID at the given block's location.
   *
   * <p>Returns {@code -1L} when GriefPrevention is not installed, not enabled,
   * the block is in the wilderness, or any reflective lookup fails.</p>
   *
   * @param plugin the plugin instance
   * @param block  the block whose location is checked
   * @return the claim ID, or {@code -1L} if unavailable
   */
  public static long getClaimIdAt(DoubleDoors plugin, Block block) {
    Plugin griefPrevention = Bukkit.getPluginManager().getPlugin("GriefPrevention");
    if (griefPrevention == null || !griefPrevention.isEnabled()) {
      return -1L;
    }
    try {
      Object dataStore = getDataStore(griefPrevention);
      if (dataStore == null) {
        return -1L;
      }
      Object claim = findClaimAt(dataStore, block);
      if (claim == null) {
        return -1L;
      }
      return extractClaimId(claim);
    } catch (ReflectiveOperationException ex) {
      plugin.getLogger().fine("GriefPrevention claim-ID lookup failed: %s".formatted(ex.getMessage()));
      return -1L;
    }
  }

  /**
   * Returns whether the given player has claim-management rights at the block's location.
   *
   * <p>A player is considered a manager if they have build trust (or higher) in the claim,
   * or if GriefPrevention is not installed. Fails open on any reflective error.</p>
   *
   * @param plugin the plugin instance
   * @param player the player to check
   * @param block  the block whose claim is evaluated
   * @return true if the player may manage the claim
   */
  public static boolean isClaimManagerAt(DoubleDoors plugin, Player player, Block block) {
    Plugin griefPrevention = Bukkit.getPluginManager().getPlugin("GriefPrevention");
    if (griefPrevention == null || !griefPrevention.isEnabled()) {
      return true;
    }
    try {
      Object dataStore = getDataStore(griefPrevention);
      if (dataStore == null) {
        return true;
      }
      Object claim = findClaimAt(dataStore, block);
      if (claim == null) {
        return true; // wilderness — no restrictions
      }
      Boolean allowBuild = tryAllowBuild(claim, player, block.getType());
      if (allowBuild != null) {
        return allowBuild;
      }
      Boolean checkPerm = tryCheckPermission(claim, player);
      if (checkPerm != null) {
        return checkPerm;
      }
    } catch (ReflectiveOperationException ex) {
      plugin.getLogger().fine("GriefPrevention claim-manager check failed: %s".formatted(ex.getMessage()));
    }
    return true;
  }

  private static long extractClaimId(Object claim) throws ReflectiveOperationException {
    for (Method method : claim.getClass().getMethods()) {
      if (method.getName().equals("getID") && method.getParameterCount() == 0) {
        Object result = method.invoke(claim);
        if (result instanceof Long l) {
          return l;
        }
        if (result instanceof Number n) {
          return n.longValue();
        }
      }
    }
    try {
      Field idField = claim.getClass().getDeclaredField("id");
      idField.setAccessible(true);
      Object val = idField.get(claim);
      if (val instanceof Long l) {
        return l;
      }
      if (val instanceof Number n) {
        return n.longValue();
      }
    } catch (NoSuchFieldException ignored) {
      // fall through
    }
    return -1L;
  }

  private static String evaluateLocationFilters(PluginConfig config, Block block) {
    String normalized = normalizeLocation(block);
    PluginConfig.LocationMode locationMode = config.getLocationMode();
    Set<String> locationEntries = config.getLocationEntries();
    if (locationMode == PluginConfig.LocationMode.BLACKLIST && locationEntries.contains(normalized)) {
      return "location_blacklist";
    }
    if (locationMode == PluginConfig.LocationMode.WHITELIST && !locationEntries.contains(normalized)) {
      return "location_not_whitelisted";
    }

    Set<String> regionIds = getWorldGuardRegionIds(block);
    if (!regionIds.isEmpty()) {
      PluginConfig.RegionMode regionMode = config.getWorldGuardRegionMode();
      Set<String> configuredRegionIds = config.getWorldGuardRegionIds();
      boolean anyConfigured = regionIds.stream().anyMatch(configuredRegionIds::contains);
      if (regionMode == PluginConfig.RegionMode.BLACKLIST && anyConfigured) {
        return "worldguard_region_blacklist";
      }
      if (regionMode == PluginConfig.RegionMode.WHITELIST && !anyConfigured) {
        return "worldguard_region_not_whitelisted";
      }
    }
    return "";
  }

  private static String evaluateWorldGuard(PluginConfig config, Player player, Block block) {
    Plugin worldGuardPlugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
    if (worldGuardPlugin == null || !worldGuardPlugin.isEnabled()) {
      return "";
    }

    if (config.isWorldGuardRespectBuildPermission()) {
      try {
        Class<?> wgPluginClass = Class.forName("com.sk89q.worldguard.bukkit.WorldGuardPlugin");
        Method inst = wgPluginClass.getMethod("inst");
        Object wgPlugin = inst.invoke(null);
        Method canBuild = wgPluginClass.getMethod("canBuild", Player.class, org.bukkit.Location.class);
        Object allowed = canBuild.invoke(wgPlugin, player, block.getLocation());
        if (allowed instanceof Boolean bool && !bool) {
          return "worldguard_build_denied";
        }
      } catch (ReflectiveOperationException ex) {
        // Fail open to keep compatibility resilient.
      }
    }

    String customFlag = config.getWorldGuardCustomFlag();
    if (!customFlag.isEmpty()) {
      String state = resolveWorldGuardCustomFlagState(block, customFlag);
      if ("deny".equals(state)) {
        return "worldguard_custom_flag_deny";
      }
      if ("allow".equals(state)) {
        return "";
      }
    }
    return "";
  }

  private static String resolveWorldGuardCustomFlagState(Block block, String flagName) {
    try {
      Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
      Object worldGuard = worldGuardClass.getMethod("getInstance").invoke(null);
      Object registry = worldGuard.getClass().getMethod("getFlagRegistry").invoke(worldGuard);
      Object flag = registry.getClass().getMethod("get", String.class).invoke(registry, flagName);
      if (flag == null) {
        return "";
      }

      for (Object region : getApplicableWorldGuardRegions(block)) {
        try {
          Object value = region.getClass().getMethod("getFlag", flag.getClass()).invoke(region, flag);
          if (value == null) {
            continue;
          }
          String normalized = value.toString().trim().toLowerCase(Locale.ROOT);
          if (normalized.contains("deny")) {
            return "deny";
          }
          if (normalized.contains("allow")) {
            return "allow";
          }
        } catch (ReflectiveOperationException ignored) {
          // Move to the next region.
        }
      }
    } catch (ReflectiveOperationException ignored) {
      // Fail open.
    }
    return "";
  }

  private static Set<String> getWorldGuardRegionIds(Block block) {
    Set<String> ids = new HashSet<>();
    for (Object region : getApplicableWorldGuardRegions(block)) {
      try {
        Object id = region.getClass().getMethod("getId").invoke(region);
        if (id != null) {
          ids.add(id.toString().trim().toLowerCase(Locale.ROOT));
        }
      } catch (ReflectiveOperationException ignored) {
        // Best-effort region-id collection.
      }
    }
    return ids;
  }

  private static Iterable<?> getApplicableWorldGuardRegions(Block block) {
    try {
      Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
      Object worldGuard = worldGuardClass.getMethod("getInstance").invoke(null);
      Object platform = worldGuard.getClass().getMethod("getPlatform").invoke(worldGuard);
      Object regionContainer = platform.getClass().getMethod("getRegionContainer").invoke(platform);

      Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
      Object weWorld = bukkitAdapterClass.getMethod("adapt", org.bukkit.World.class).invoke(null, block.getWorld());
      Object regionManager = null;
      for (Method method : regionContainer.getClass().getMethods()) {
        if (!method.getName().equals("get") || method.getParameterCount() != 1) {
          continue;
        }
        Class<?> parameterType = method.getParameterTypes()[0];
        if (!parameterType.isInstance(weWorld) && !parameterType.isAssignableFrom(weWorld.getClass())) {
          continue;
        }
        regionManager = method.invoke(regionContainer, weWorld);
        break;
      }
      if (regionManager == null) {
        return Set.of();
      }

      Class<?> blockVectorClass = Class.forName("com.sk89q.worldedit.math.BlockVector3");
      Object blockVector = blockVectorClass.getMethod("at", int.class, int.class, int.class)
          .invoke(null, block.getX(), block.getY(), block.getZ());
      Object applicable = regionManager.getClass().getMethod("getApplicableRegions", blockVectorClass).invoke(regionManager, blockVector);
      if (applicable instanceof Iterable<?> iterable) {
        return iterable;
      }
    } catch (ReflectiveOperationException ignored) {
      // Fail open.
    }

    return Set.of();
  }

  private static String normalizeLocation(Block block) {
    return block.getWorld().getName().toLowerCase(Locale.ROOT)
        + ":" + block.getX()
        + ":" + block.getY()
        + ":" + block.getZ();
  }
}

