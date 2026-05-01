package me.szabee.doubledoors.config;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.configuration.file.YamlConfiguration;

import me.szabee.doubledoors.DoubleDoors;
import me.szabee.doubledoors.storage.SharedSqlStorage;
import me.szabee.doubledoors.util.SchedulerBridge;

/**
 * Manages per-player preferences, persisted to {@code players.yml} inside the plugin data folder.
 *
 * <p>Each player can independently enable/disable linked-opening for the plugin overall,
 * or for specific block types (doors, fence gates, trapdoors).</p>
 */
public final class PlayerPreferences {

  private final DoubleDoors plugin;
  private final File dataFile;
  private final SharedSqlStorage sqlStorage;
  private final boolean useSql;
  private final Map<UUID, PlayerPref> cache = new ConcurrentHashMap<>();

  /**
   * Loads player preferences from {@code players.yml}.
   *
   * @param plugin the plugin instance
   */
  public PlayerPreferences(DoubleDoors plugin) {
    this.plugin = plugin;
    this.dataFile = new File(plugin.getDataFolder(), "players.yml");
    this.sqlStorage = plugin.getSqlStorage();
    this.useSql = plugin.getPluginConfig().isSqlEnabled() && sqlStorage != null;
    load();
  }

  /**
   * Reloads all preferences from disk, clearing the in-memory cache.
   */
  public void load() {
    if (useSql) {
      cache.clear();
      for (Map.Entry<UUID, SharedSqlStorage.SqlPlayerPref> entry : sqlStorage.loadAllPlayerPreferences().entrySet()) {
        SharedSqlStorage.SqlPlayerPref pref = entry.getValue();
        cache.put(entry.getKey(), new PlayerPref(
            pref.enabled(),
            pref.enableDoors(),
            pref.enableFenceGates(),
            pref.enableTrapdoors(),
            pref.enableAutoClose(),
            pref.enableKnockSound()));
      }
      return;
    }

    YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
    cache.clear();
    for (String key : data.getKeys(false)) {
      // Fast path: skip obviously-invalid UUID formats before attempting UUID.fromString.
      if (key.length() != 36
          || key.charAt(8) != '-'
          || key.charAt(13) != '-'
          || key.charAt(18) != '-'
          || key.charAt(23) != '-') {
        continue;
      }
      try {
        UUID uuid = UUID.fromString(key);
        boolean enabled = data.getBoolean(key + ".enabled", true);
        boolean doors = data.getBoolean(key + ".enableDoors", true);
        boolean gates = data.getBoolean(key + ".enableFenceGates", true);
        boolean trapdoors = data.getBoolean(key + ".enableTrapdoors", true);
        boolean autoClose = data.getBoolean(key + ".enableAutoClose", true);
        boolean knockSound = data.getBoolean(key + ".enableKnockSound", true);
        cache.put(uuid, new PlayerPref(enabled, doors, gates, trapdoors, autoClose, knockSound));
      } catch (IllegalArgumentException ignored) {
        // Non-UUID top-level key — skip silently.
      }
    }
  }

  /**
   * Saves all in-memory preferences synchronously to {@code players.yml}.
   */
  public void save() {
    if (useSql) {
      return;
    }

    YamlConfiguration data = new YamlConfiguration();
    for (Map.Entry<UUID, PlayerPref> entry : Map.copyOf(cache).entrySet()) {
      String key = entry.getKey().toString();
      PlayerPref pref = entry.getValue();
      data.set(key + ".enabled", pref.enabled());
      data.set(key + ".enableDoors", pref.enableDoors());
      data.set(key + ".enableFenceGates", pref.enableFenceGates());
      data.set(key + ".enableTrapdoors", pref.enableTrapdoors());
      data.set(key + ".enableAutoClose", pref.enableAutoClose());
      data.set(key + ".enableKnockSound", pref.enableKnockSound());
    }
    try {
      data.save(dataFile);
    } catch (IOException e) {
      plugin.getLogger().warning("Could not save players.yml: %s".formatted(e.getMessage()));
    }
  }

  /** Saves asynchronously; safe to call from the main thread after every mutation. */
  private void saveAsync(UUID changedUuid) {
    if (useSql) {
      PlayerPref pref = cache.get(changedUuid);
      if (pref == null) {
        return;
      }
      SharedSqlStorage.SqlPlayerPref snapshot = new SharedSqlStorage.SqlPlayerPref(
          pref.enabled(),
          pref.enableDoors(),
          pref.enableFenceGates(),
          pref.enableTrapdoors(),
          pref.enableAutoClose(),
          pref.enableKnockSound());
      SchedulerBridge.runAsync(plugin, () -> sqlStorage.savePlayerPreference(changedUuid, snapshot));
      return;
    }

    SchedulerBridge.runAsync(plugin, this::save);
  }

  private PlayerPref getOrDefault(UUID uuid) {
    return cache.computeIfAbsent(uuid, k -> new PlayerPref(true, true, true, true, true, true));
  }

  /** Returns whether the player has not globally disabled linked-door behavior. */
  public boolean isEnabled(UUID uuid) {
    return getOrDefault(uuid).enabled();
  }

  /** Returns whether the player has doors linking enabled. */
  public boolean isDoorsEnabled(UUID uuid) {
    return getOrDefault(uuid).enableDoors();
  }

  /** Returns whether the player has fence-gate linking enabled. */
  public boolean isFenceGatesEnabled(UUID uuid) {
    return getOrDefault(uuid).enableFenceGates();
  }

  /** Returns whether the player has trapdoor linking enabled. */
  public boolean isTrapdoorsEnabled(UUID uuid) {
    return getOrDefault(uuid).enableTrapdoors();
  }

  /** Returns whether the player has auto-close enabled for their interactions. */
  public boolean isAutoCloseEnabled(UUID uuid) {
    return getOrDefault(uuid).enableAutoClose();
  }

  /** Returns whether the player has knock sound enabled for their interactions. */
  public boolean isKnockSoundEnabled(UUID uuid) {
    return getOrDefault(uuid).enableKnockSound();
  }

  /**
   * Toggles the player's global linked-door on/off switch.
   *
   * @param uuid the player's unique ID
   * @return {@code true} if the feature is now enabled, {@code false} if now disabled
   */
  public boolean toggleAll(UUID uuid) {
    PlayerPref current = getOrDefault(uuid);
    boolean next = !current.enabled();
    cache.put(uuid, new PlayerPref(
        next,
        current.enableDoors(),
        current.enableFenceGates(),
        current.enableTrapdoors(),
        current.enableAutoClose(),
        current.enableKnockSound()));
    saveAsync(uuid);
    return next;
  }

  /**
   * Toggles the door-linking preference for the given player.
   *
   * @param uuid the player's unique ID
   * @return the new enabled state
   */
  public boolean toggleDoors(UUID uuid) {
    PlayerPref current = getOrDefault(uuid);
    boolean next = !current.enableDoors();
    cache.put(uuid, new PlayerPref(
        current.enabled(),
        next,
        current.enableFenceGates(),
        current.enableTrapdoors(),
        current.enableAutoClose(),
        current.enableKnockSound()));
    saveAsync(uuid);
    return next;
  }

  /**
   * Toggles the fence-gate-linking preference for the given player.
   *
   * @param uuid the player's unique ID
   * @return the new enabled state
   */
  public boolean toggleFenceGates(UUID uuid) {
    PlayerPref current = getOrDefault(uuid);
    boolean next = !current.enableFenceGates();
    cache.put(uuid, new PlayerPref(
        current.enabled(),
        current.enableDoors(),
        next,
        current.enableTrapdoors(),
        current.enableAutoClose(),
        current.enableKnockSound()));
    saveAsync(uuid);
    return next;
  }

  /**
   * Toggles the trapdoor-linking preference for the given player.
   *
   * @param uuid the player's unique ID
   * @return the new enabled state
   */
  public boolean toggleTrapdoors(UUID uuid) {
    PlayerPref current = getOrDefault(uuid);
    boolean next = !current.enableTrapdoors();
    cache.put(uuid, new PlayerPref(
        current.enabled(),
        current.enableDoors(),
        current.enableFenceGates(),
        next,
        current.enableAutoClose(),
        current.enableKnockSound()));
    saveAsync(uuid);
    return next;
  }

  /**
   * Toggles auto-close preference for the given player.
   *
   * @param uuid the player's unique ID
   * @return the new enabled state
   */
  public boolean toggleAutoClose(UUID uuid) {
    PlayerPref current = getOrDefault(uuid);
    boolean next = !current.enableAutoClose();
    cache.put(uuid, new PlayerPref(
        current.enabled(),
        current.enableDoors(),
        current.enableFenceGates(),
        current.enableTrapdoors(),
        next,
        current.enableKnockSound()));
    saveAsync(uuid);
    return next;
  }

  /**
   * Toggles knock-sound preference for the given player.
   *
   * @param uuid the player's unique ID
   * @return the new enabled state
   */
  public boolean toggleKnockSound(UUID uuid) {
    PlayerPref current = getOrDefault(uuid);
    boolean next = !current.enableKnockSound();
    cache.put(uuid, new PlayerPref(
        current.enabled(),
        current.enableDoors(),
        current.enableFenceGates(),
        current.enableTrapdoors(),
        current.enableAutoClose(),
        next));
    saveAsync(uuid);
    return next;
  }

  /**
   * Immutable snapshot of a single player's preferences.
   *
   * @param enabled          global linked-door on/off
   * @param enableDoors      door-linking on/off
   * @param enableFenceGates fence-gate-linking on/off
   * @param enableTrapdoors  trapdoor-linking on/off
   * @param enableAutoClose  per-player auto-close on/off
   * @param enableKnockSound per-player knock sound on/off
   */
  public record PlayerPref(
      boolean enabled,
      boolean enableDoors,
      boolean enableFenceGates,
      boolean enableTrapdoors,
      boolean enableAutoClose,
      boolean enableKnockSound
  ) {}
}

