package me.szabee.doubledoors.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;

import me.szabee.doubledoors.DoubleDoors;
import me.szabee.doubledoors.util.OpenableType;
import me.szabee.doubledoors.util.SchedulerBridge;

/**
 * Configuration wrapper for DoubleDoors.
 */
public final class PluginConfig {
  private final DoubleDoors plugin;
  private volatile ConfigSnapshot snapshot = ConfigSnapshot.defaults();

  /**
   * Creates and loads a plugin config wrapper.
   *
   * @param plugin the plugin instance
   */
  public PluginConfig(DoubleDoors plugin) {
    this.plugin = plugin;
    reload();
  }

  /**
   * Reloads config values from config.yml.
   */
  public void reload() {
    boolean enableRecursiveOpening = plugin.getConfig().getBoolean("enableRecursiveOpening", true);
    int recursiveOpeningMaxBlocksDistance = plugin.getConfig().getInt("recursiveOpeningMaxBlocksDistance", 10);
    if (recursiveOpeningMaxBlocksDistance < 1) {
      recursiveOpeningMaxBlocksDistance = 1;
    }
    if (recursiveOpeningMaxBlocksDistance > 32) {
      recursiveOpeningMaxBlocksDistance = 32;
    }

    boolean enableDoors = plugin.getConfig().getBoolean("enableDoors", true);
    boolean enableFenceGates = plugin.getConfig().getBoolean("enableFenceGates", true);
    boolean enableTrapdoors = plugin.getConfig().getBoolean("enableTrapdoors", true);
    boolean enableVillagerLinkedDoors = plugin.getConfig().getBoolean("enableVillagerLinkedDoors", true);
    boolean enableAutoClose = plugin.getConfig().getBoolean("enableAutoClose", false);
    int autoCloseDelaySeconds = plugin.getConfig().getInt("autoCloseDelaySeconds", 5);
    if (autoCloseDelaySeconds < 1) {
      autoCloseDelaySeconds = 1;
    }
    if (autoCloseDelaySeconds > 300) {
      autoCloseDelaySeconds = 300;
    }
    boolean enableKnockFeature = plugin.getConfig().getBoolean("enableKnockFeature", true);
    int knockDistanceBlocks = plugin.getConfig().getInt("knockDistanceBlocks", 12);
    if (knockDistanceBlocks < 1) {
      knockDistanceBlocks = 1;
    }
    if (knockDistanceBlocks > 64) {
      knockDistanceBlocks = 64;
    }
    boolean serverWideEnabled = plugin.getConfig().getBoolean("serverWideEnabled", true);
    boolean playPartnerSound = plugin.getConfig().getBoolean("playPartnerSound", true);
    boolean enablePartnerParticles = plugin.getConfig().getBoolean("enablePartnerParticles", false);

    String genericSoundRaw = plugin.getConfig().getString("partnerSound", "");
    Sound genericPartnerSound = parseSound(genericSoundRaw);
    Sound doorPartnerSound = parseSound(plugin.getConfig().getString("partnerSoundOverrides.doors", ""));
    Sound gatePartnerSound = parseSound(plugin.getConfig().getString("partnerSoundOverrides.fenceGates", ""));
    Sound trapdoorPartnerSound = parseSound(plugin.getConfig().getString("partnerSoundOverrides.trapdoors", ""));

    String particleName = plugin.getConfig().getString("partnerParticle", "END_ROD");
    Particle partnerParticle = parseParticle(particleName, Particle.END_ROD);
    int partnerParticleCount = plugin.getConfig().getInt("partnerParticleCount", 6);
    if (partnerParticleCount < 1) {
      partnerParticleCount = 1;
    }
    if (partnerParticleCount > 64) {
      partnerParticleCount = 64;
    }

    String previewParticleName = plugin.getConfig().getString("previewParticle", "WAX_OFF");
    Particle previewParticle = parseParticle(previewParticleName, Particle.WAX_OFF);
    int previewParticleCount = plugin.getConfig().getInt("previewParticleCount", 18);
    if (previewParticleCount < 1) {
      previewParticleCount = 1;
    }
    if (previewParticleCount > 128) {
      previewParticleCount = 128;
    }
    int previewDurationTicks = plugin.getConfig().getInt("previewDurationTicks", 60);
    if (previewDurationTicks < 20) {
      previewDurationTicks = 20;
    }
    if (previewDurationTicks > 200) {
      previewDurationTicks = 200;
    }

    int extraAnimationDelayTicks = plugin.getConfig().getInt("animationSyncExtraDelayTicks", 0);
    if (extraAnimationDelayTicks < 0) {
      extraAnimationDelayTicks = 0;
    }
    if (extraAnimationDelayTicks > 4) {
      extraAnimationDelayTicks = 4;
    }

    int lookupCacheTtlMillis = plugin.getConfig().getInt("lookupCacheTtlMillis", 1200);
    if (lookupCacheTtlMillis < 200) {
      lookupCacheTtlMillis = 200;
    }
    if (lookupCacheTtlMillis > 10_000) {
      lookupCacheTtlMillis = 10_000;
    }
    int interactionCooldownMillis = plugin.getConfig().getInt("interactionCooldownMillis", 250);
    if (interactionCooldownMillis < 0) {
      interactionCooldownMillis = 0;
    }
    if (interactionCooldownMillis > 5000) {
      interactionCooldownMillis = 5000;
    }

    LocationMode locationMode = parseLocationMode(plugin.getConfig().getString("locationFilter.mode", "DISABLED"));
    Set<String> configuredLocations = normalizeLocationEntries(plugin.getConfig().getStringList("locationFilter.locations"));

    RegionMode regionMode = parseRegionMode(plugin.getConfig().getString("worldGuardRegionFilter.mode", "DISABLED"));
    Set<String> configuredRegions = normalizeRegionEntries(plugin.getConfig().getStringList("worldGuardRegionFilter.regions"));
    String worldGuardCustomFlag = normalizeLower(plugin.getConfig().getString("worldGuardCustomFlag", "double-doors-allow"));
    boolean worldGuardRespectBuildPermission = plugin.getConfig().getBoolean("worldGuardRespectBuildPermission", true);
    boolean worldGuardRespectUseFlag = plugin.getConfig().getBoolean("worldGuardRespectUseFlag", true);

    boolean enableAnonymousTracking = plugin.getConfig().getBoolean("enableAnonymousTracking", true);
    boolean enableExtendedAnonymousTracking = plugin.getConfig().getBoolean("enableExtendedAnonymousTracking", false);

    List<String> configuredCountries = plugin.getConfig().getStringList("trackingCountries");
    List<String> trackingCountries = new ArrayList<>();
    for (String country : configuredCountries) {
      if (country != null && !country.isBlank()) {
        trackingCountries.add(country.trim());
      }
    }

    String trackingServerLocation = plugin.getConfig().getString("trackingServerLocation", "");
    if (trackingServerLocation == null) {
      trackingServerLocation = "";
    }
    trackingServerLocation = trackingServerLocation.trim();

    boolean sqlEnabled = plugin.getConfig().getBoolean("sql.enabled", false);
    String sqlJdbcUrl = plugin.getConfig().getString("sql.jdbcUrl", "jdbc:sqlite:plugins/DoubleDoors/doubledoors.db");
    if (sqlJdbcUrl == null || sqlJdbcUrl.isBlank()) {
      sqlJdbcUrl = "jdbc:sqlite:plugins/DoubleDoors/doubledoors.db";
    }
    String sqlUsername = plugin.getConfig().getString("sql.username", "");
    if (sqlUsername == null) {
      sqlUsername = "";
    }
    String sqlPassword = plugin.getConfig().getString("sql.password", "");
    if (sqlPassword == null) {
      sqlPassword = "";
    }
    boolean migrateYamlToSql = plugin.getConfig().getBoolean("sql.migrateFromYaml", true);
    long heartbeatSeconds = plugin.getConfig().getLong("sql.proxyHeartbeatMaxAgeSeconds", 180L);
    if (heartbeatSeconds < 15L) {
      heartbeatSeconds = 15L;
    }
    long proxyHeartbeatMaxAgeMillis = heartbeatSeconds * 1000L;

    boolean updateCheckerEnabled = plugin.getConfig().getBoolean("updateChecker.enabled", true);
    int updateCheckerScheduleSeconds = plugin.getConfig().getInt("updateChecker.checkScheduleSeconds", 3600);
    if (updateCheckerScheduleSeconds < 300) {
      updateCheckerScheduleSeconds = 300;
    }
    if (updateCheckerScheduleSeconds > 86_400) {
      updateCheckerScheduleSeconds = 86_400;
    }
    boolean updateCheckerNotify = plugin.getConfig().getBoolean("updateChecker.notify", true);

    String configuredLanguage = plugin.getConfig().getString("language", "en_US");
    if (configuredLanguage == null || configuredLanguage.isBlank()) {
      configuredLanguage = "en_US";
    }
    String language = configuredLanguage.trim();

    snapshot = new ConfigSnapshot(
        enableRecursiveOpening,
        recursiveOpeningMaxBlocksDistance,
        enableDoors,
        enableFenceGates,
        enableTrapdoors,
        enableVillagerLinkedDoors,
        enableAutoClose,
        autoCloseDelaySeconds,
        enableKnockFeature,
        knockDistanceBlocks,
        serverWideEnabled,
        playPartnerSound,
        genericPartnerSound,
        doorPartnerSound,
        gatePartnerSound,
        trapdoorPartnerSound,
        enablePartnerParticles,
        partnerParticle,
        partnerParticleCount,
        previewParticle,
        previewParticleCount,
        previewDurationTicks,
        extraAnimationDelayTicks,
        lookupCacheTtlMillis,
        interactionCooldownMillis,
        locationMode,
        Set.copyOf(configuredLocations),
        regionMode,
        Set.copyOf(configuredRegions),
        worldGuardCustomFlag,
        worldGuardRespectBuildPermission,
        worldGuardRespectUseFlag,
        enableAnonymousTracking,
        enableExtendedAnonymousTracking,
        List.copyOf(trackingCountries),
        trackingServerLocation,
        language,
        sqlEnabled,
        sqlJdbcUrl,
        sqlUsername,
        sqlPassword,
        migrateYamlToSql,
        proxyHeartbeatMaxAgeMillis,
        updateCheckerEnabled,
        updateCheckerScheduleSeconds,
        updateCheckerNotify);
  }

  /**
   * Gets whether recursive opening is enabled.
   *
   * @return true when recursive opening is enabled
   */
  public boolean isEnableRecursiveOpening() {
    return snapshot.enableRecursiveOpening();
  }

  /**
   * Gets the recursive max distance.
   *
   * @return max recursive distance between 1 and 32
   */
  public int getRecursiveOpeningMaxBlocksDistance() {
    return snapshot.recursiveOpeningMaxBlocksDistance();
  }

  /**
   * Gets whether door support is enabled.
   *
   * @return true when door support is enabled
   */
  public boolean isEnableDoors() {
    return snapshot.enableDoors();
  }

  /**
   * Gets whether fence gate support is enabled.
   *
   * @return true when fence gate support is enabled
   */
  public boolean isEnableFenceGates() {
    return snapshot.enableFenceGates();
  }

  /**
   * Gets whether trapdoor support is enabled.
   *
   * @return true when trapdoor support is enabled
   */
  public boolean isEnableTrapdoors() {
    return snapshot.enableTrapdoors();
  }

  /**
   * Gets whether villager-triggered linked-door behavior is enabled.
   *
   * @return true when villager linked-door behavior is enabled
   */
  public boolean isEnableVillagerLinkedDoors() {
    return snapshot.enableVillagerLinkedDoors();
  }

  /**
   * Gets whether automatic closing is enabled for player-triggered opens.
   *
   * @return true when auto-close is enabled
   */
  public boolean isEnableAutoClose() {
    return snapshot.enableAutoClose();
  }

  /**
   * Gets the auto-close delay in seconds.
   *
   * @return auto-close delay in seconds
   */
  public int getAutoCloseDelaySeconds() {
    return snapshot.autoCloseDelaySeconds();
  }

  /**
   * Gets whether left-click knock sounds are enabled.
   *
   * @return true when knock behavior is enabled
   */
  public boolean isEnableKnockFeature() {
    return snapshot.enableKnockFeature();
  }

  /**
   * Gets how far knock sounds can be heard in blocks.
   *
   * @return knock hearing distance in blocks
   */
  public int getKnockDistanceBlocks() {
    return snapshot.knockDistanceBlocks();
  }

  /**
   * Gets whether linked opening is enabled globally for the server.
   *
   * @return true when server-wide behavior is enabled
   */
  public boolean isServerWideEnabled() {
    return snapshot.serverWideEnabled();
  }

  /**
   * Gets whether partner-door sounds should be played for mirrored updates.
   *
   * @return true when partner sounds are enabled
   */
  public boolean isPlayPartnerSound() {
    return snapshot.playPartnerSound();
  }

  /**
   * Resolves the configured partner sound for a type.
   *
   * @param type the block type category
   * @return configured sound or null to use vanilla/no explicit sound
   */
  public Sound getPartnerSound(OpenableType type) {
    return switch (type) {
      case DOOR -> snapshot.doorPartnerSound() != null ? snapshot.doorPartnerSound() : snapshot.genericPartnerSound();
      case FENCE_GATE -> snapshot.gatePartnerSound() != null ? snapshot.gatePartnerSound() : snapshot.genericPartnerSound();
      case TRAPDOOR -> snapshot.trapdoorPartnerSound() != null ? snapshot.trapdoorPartnerSound() : snapshot.genericPartnerSound();
      case CUSTOM -> snapshot.genericPartnerSound();
    };
  }

  /**
   * Gets whether partner particles are enabled.
   *
   * @return true when partner particles should be emitted
   */
  public boolean isEnablePartnerParticles() {
    return snapshot.enablePartnerParticles();
  }

  /**
   * Gets the particle type used for linked partner effects.
   *
   * @return partner effect particle type
   */
  public Particle getPartnerParticle() {
    return snapshot.partnerParticle();
  }

  /**
   * Gets the particle count used for linked partner effects.
   *
   * @return partner particle count
   */
  public int getPartnerParticleCount() {
    return snapshot.partnerParticleCount();
  }

  /**
   * Gets the particle type used by the preview command.
   *
   * @return preview particle type
   */
  public Particle getPreviewParticle() {
    return snapshot.previewParticle();
  }

  /**
   * Gets the preview particle count.
   *
   * @return preview particle count
   */
  public int getPreviewParticleCount() {
    return snapshot.previewParticleCount();
  }

  /**
   * Gets how long preview particles should run.
   *
   * @return preview duration in ticks
   */
  public int getPreviewDurationTicks() {
    return snapshot.previewDurationTicks();
  }

  /**
   * Gets extra delay ticks used for animation sync compensation.
   *
   * @return extra ticks between 0 and 4
   */
  public int getAnimationSyncExtraDelayTicks() {
    return snapshot.extraAnimationDelayTicks();
  }

  /**
   * Gets the lookup cache TTL in milliseconds.
   *
   * @return lookup cache TTL in milliseconds
   */
  public int getLookupCacheTtlMillis() {
    return snapshot.lookupCacheTtlMillis();
  }

  /**
   * Gets the interaction cooldown in milliseconds for repeated same-door toggles.
   *
   * @return interaction cooldown in milliseconds (0 disables)
   */
  public int getInteractionCooldownMillis() {
    return snapshot.interactionCooldownMillis();
  }

  /**
   * Gets the location filter mode.
   *
   * @return location mode
   */
  public LocationMode getLocationMode() {
    return snapshot.locationMode();
  }

  /**
   * Gets normalized world:x:y:z location filter entries.
   *
   * @return immutable set of normalized entries
   */
  public Set<String> getLocationEntries() {
    return snapshot.locationEntries();
  }

  /**
   * Gets the WorldGuard region filter mode.
   *
   * @return WorldGuard region mode
   */
  public RegionMode getWorldGuardRegionMode() {
    return snapshot.regionMode();
  }

  /**
   * Gets normalized WorldGuard region IDs used by region filters.
   *
   * @return immutable set of region IDs
   */
  public Set<String> getWorldGuardRegionIds() {
    return snapshot.regionIds();
  }

  /**
   * Gets the optional WorldGuard custom state-flag name.
   *
   * @return normalized custom flag name, or empty string
   */
  public String getWorldGuardCustomFlag() {
    return snapshot.worldGuardCustomFlag();
  }

  /**
   * Gets whether WorldGuard build checks should be enforced.
   *
   * @return true when build checks should run
   */
  public boolean isWorldGuardRespectBuildPermission() {
    return snapshot.worldGuardRespectBuildPermission();
  }

  /**
   * Gets whether WorldGuard use-flag checks should be enforced.
   *
   * @return true when use-flag checks should run
   */
  public boolean isWorldGuardRespectUseFlag() {
    return snapshot.worldGuardRespectUseFlag();
  }

  /**
   * Gets whether anonymous tracking is enabled.
   *
   * @return true when FastStats tracking is enabled
   */
  public boolean isEnableAnonymousTracking() {
    return snapshot.enableAnonymousTracking();
  }

  /**
   * Gets whether extended anonymous tracking is enabled.
   *
   * @return true when extra telemetry should be sent
   */
  public boolean isEnableExtendedAnonymousTracking() {
    return snapshot.enableExtendedAnonymousTracking();
  }

  /**
   * Gets the configured countries associated with this server.
   *
   * @return immutable list of configured country codes
   */
  public List<String> getTrackingCountries() {
    return snapshot.trackingCountries();
  }

  /**
   * Gets the configured server location label.
   *
   * @return trimmed location label, or empty string
   */
  public String getTrackingServerLocation() {
    return snapshot.trackingServerLocation();
  }

  /**
   * Gets the configured plugin language code.
   *
   * @return language code such as {@code en_US}
   */
  public String getLanguage() {
    return snapshot.language();
  }

  /**
   * Gets whether SQL-backed storage is enabled.
   *
   * @return true when SQL storage should be used
   */
  public boolean isSqlEnabled() {
    return snapshot.sqlEnabled();
  }

  /**
   * Gets the JDBC URL used by both Bukkit and proxy modules.
   *
   * @return JDBC URL
   */
  public String getSqlJdbcUrl() {
    return snapshot.sqlJdbcUrl();
  }

  /**
   * Gets the SQL username.
   *
   * @return SQL username, or empty string
   */
  public String getSqlUsername() {
    return snapshot.sqlUsername();
  }

  /**
   * Gets the SQL password.
   *
   * @return SQL password, or empty string
   */
  public String getSqlPassword() {
    return snapshot.sqlPassword();
  }

  /**
   * Gets whether YAML data should be migrated to SQL on startup.
   *
   * @return true when one-time migration should run if needed
   */
  public boolean isMigrateYamlToSql() {
    return snapshot.migrateYamlToSql();
  }

  /**
   * Gets the maximum accepted age for proxy heartbeats in milliseconds.
   *
   * @return heartbeat max age in milliseconds
   */
  public long getProxyHeartbeatMaxAgeMillis() {
    return snapshot.proxyHeartbeatMaxAgeMillis();
  }

  /**
   * Gets whether startup update checks are enabled.
   *
   * @return true when the built-in updater is enabled
   */
  public boolean isUpdateCheckerEnabled() {
    return snapshot.updateCheckerEnabled();
  }

  /**
   * Gets the number of seconds between updater checks.
   *
   * @return check schedule in seconds
   */
  public int getUpdateCheckerScheduleSeconds() {
    return snapshot.updateCheckerScheduleSeconds();
  }

  /**
   * Gets whether update notifications should be sent.
   *
   * @return true when player notifications are enabled
   */
  public boolean isUpdateCheckerNotify() {
    return snapshot.updateCheckerNotify();
  }

  /**
   * Sets and persists whether linked opening is enabled globally for the server.
   *
   * @param enabled true to enable globally, false to disable globally
   */
  public void setServerWideEnabled(boolean enabled) {
    snapshot = snapshot.withServerWideEnabled(enabled);
    plugin.getConfig().set("serverWideEnabled", enabled);
    SchedulerBridge.runAsync(plugin, plugin::saveConfig);
  }

  private record ConfigSnapshot(
      boolean enableRecursiveOpening,
      int recursiveOpeningMaxBlocksDistance,
      boolean enableDoors,
      boolean enableFenceGates,
      boolean enableTrapdoors,
      boolean enableVillagerLinkedDoors,
      boolean enableAutoClose,
      int autoCloseDelaySeconds,
      boolean enableKnockFeature,
      int knockDistanceBlocks,
      boolean serverWideEnabled,
      boolean playPartnerSound,
      Sound genericPartnerSound,
      Sound doorPartnerSound,
      Sound gatePartnerSound,
      Sound trapdoorPartnerSound,
      boolean enablePartnerParticles,
      Particle partnerParticle,
      int partnerParticleCount,
      Particle previewParticle,
      int previewParticleCount,
      int previewDurationTicks,
      int extraAnimationDelayTicks,
      int lookupCacheTtlMillis,
      int interactionCooldownMillis,
      LocationMode locationMode,
      Set<String> locationEntries,
      RegionMode regionMode,
      Set<String> regionIds,
      String worldGuardCustomFlag,
      boolean worldGuardRespectBuildPermission,
      boolean worldGuardRespectUseFlag,
      boolean enableAnonymousTracking,
      boolean enableExtendedAnonymousTracking,
      List<String> trackingCountries,
      String trackingServerLocation,
      String language,
      boolean sqlEnabled,
      String sqlJdbcUrl,
      String sqlUsername,
      String sqlPassword,
      boolean migrateYamlToSql,
      long proxyHeartbeatMaxAgeMillis,
      boolean updateCheckerEnabled,
      int updateCheckerScheduleSeconds,
      boolean updateCheckerNotify
  ) {
    private static ConfigSnapshot defaults() {
      return new ConfigSnapshot(
          true,
          10,
          true,
          true,
          false,
          5,
          true,
          12,
          true,
          true,
          true,
          true,
          null,
          null,
          null,
          null,
          false,
          Particle.END_ROD,
          6,
          Particle.WAX_OFF,
          18,
          60,
          0,
          1200,
          250,
          LocationMode.DISABLED,
          Set.of(),
          RegionMode.DISABLED,
          Set.of(),
          "double-doors-allow",
          true,
          true,
          true,
          false,
          List.of(),
          "",
          "en_US",
          false,
          "jdbc:sqlite:plugins/DoubleDoors/doubledoors.db",
          "",
          "",
          true,
          180_000L,
          true,
          3600,
          true);
    }

    private ConfigSnapshot withServerWideEnabled(boolean enabled) {
      return new ConfigSnapshot(
          enableRecursiveOpening,
          recursiveOpeningMaxBlocksDistance,
          enableDoors,
          enableFenceGates,
          enableTrapdoors,
          enableVillagerLinkedDoors,
          enableAutoClose,
          autoCloseDelaySeconds,
          enableKnockFeature,
          knockDistanceBlocks,
          enabled,
          playPartnerSound,
          genericPartnerSound,
          doorPartnerSound,
          gatePartnerSound,
          trapdoorPartnerSound,
          enablePartnerParticles,
          partnerParticle,
          partnerParticleCount,
          previewParticle,
          previewParticleCount,
          previewDurationTicks,
          extraAnimationDelayTicks,
          lookupCacheTtlMillis,
          interactionCooldownMillis,
          locationMode,
          locationEntries,
          regionMode,
          regionIds,
          worldGuardCustomFlag,
          worldGuardRespectBuildPermission,
          worldGuardRespectUseFlag,
          enableAnonymousTracking,
          enableExtendedAnonymousTracking,
          trackingCountries,
          trackingServerLocation,
          language,
          sqlEnabled,
          sqlJdbcUrl,
          sqlUsername,
          sqlPassword,
          migrateYamlToSql,
          proxyHeartbeatMaxAgeMillis,
          updateCheckerEnabled,
          updateCheckerScheduleSeconds,
          updateCheckerNotify);
    }
  }

  /**
   * Location filter mode for exact world coordinates.
   */
  public enum LocationMode {
    DISABLED,
    BLACKLIST,
    WHITELIST
  }

  /**
   * WorldGuard region filter mode.
   */
  public enum RegionMode {
    DISABLED,
    BLACKLIST,
    WHITELIST
  }

  private static Sound parseSound(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    try {
      NamespacedKey key = NamespacedKey.fromString(raw.trim().toLowerCase(Locale.ROOT));
      return key == null ? null : Registry.SOUNDS.get(key);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private static Particle parseParticle(String raw, Particle fallback) {
    if (raw == null || raw.isBlank()) {
      return fallback;
    }
    try {
      NamespacedKey key = NamespacedKey.fromString(raw.trim().toLowerCase(Locale.ROOT));
      Particle resolved = key == null ? null : Registry.PARTICLE_TYPE.get(key);
      return resolved == null ? fallback : resolved;
    } catch (IllegalArgumentException ex) {
      return fallback;
    }
  }

  private static LocationMode parseLocationMode(String raw) {
    if (raw == null) {
      return LocationMode.DISABLED;
    }
    try {
      return LocationMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      return LocationMode.DISABLED;
    }
  }

  private static RegionMode parseRegionMode(String raw) {
    if (raw == null) {
      return RegionMode.DISABLED;
    }
    try {
      return RegionMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      return RegionMode.DISABLED;
    }
  }

  private static Set<String> normalizeLocationEntries(List<String> rawEntries) {
    Set<String> normalized = new HashSet<>();
    for (String raw : rawEntries) {
      if (raw == null || raw.isBlank()) {
        continue;
      }
      String cleaned = raw.trim().toLowerCase(Locale.ROOT);
      String[] parts = cleaned.split(":");
      if (parts.length != 4) {
        continue;
      }
      try {
        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        int z = Integer.parseInt(parts[3]);
        normalized.add(parts[0] + ":" + x + ":" + y + ":" + z);
      } catch (NumberFormatException ignored) {
        // Keep filtering robust for malformed entries.
      }
    }
    return normalized;
  }

  private static Set<String> normalizeRegionEntries(List<String> rawEntries) {
    Set<String> normalized = new HashSet<>();
    for (String raw : rawEntries) {
      if (raw == null || raw.isBlank()) {
        continue;
      }
      normalized.add(raw.trim().toLowerCase(Locale.ROOT));
    }
    return normalized;
  }

  private static String normalizeLower(String raw) {
    if (raw == null || raw.isBlank()) {
      return "";
    }
    return raw.trim().toLowerCase(Locale.ROOT);
  }
}
