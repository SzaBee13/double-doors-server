package me.szabee.doubledoors.config;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.configuration.file.YamlConfiguration;

import me.szabee.doubledoors.DoubleDoors;
import me.szabee.doubledoors.storage.SharedSqlStorage;

/**
 * Manages per-player preferences, persisted to {@code players.yml} inside the plugin data folder.
 *
 * <p>Each player can independently enable/disable linked-opening for the plugin overall,
 * or for specific block types (doors, fence gates, trapdoors).</p>
 */
public final class PlayerPreferences {
  private static final double MIN_KNOCK_VOLUME = 0.0;
  private static final double MAX_KNOCK_VOLUME = 1.0;
  private static final double DEFAULT_KNOCK_VOLUME = 1.0;

  private final DoubleDoors plugin;
  private final File dataFile;
  private final SharedSqlStorage sqlStorage;
  private final boolean useSql;
  private final Map<UUID, PlayerPref> cache = new ConcurrentHashMap<>();
  private final Map<UUID, PendingSave> pendingSaves = new ConcurrentHashMap<>();
  private final Set<UUID> pendingYamlSaves = ConcurrentHashMap.newKeySet();
  private final AtomicBoolean writerScheduled = new AtomicBoolean(false);
  private final ExecutorService writerExecutor = Executors.newSingleThreadExecutor(new WriterThreadFactory());

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
      pref.enableKnockSound(),
      normalizeKnockVolume(pref.knockVolume())));
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
  double knockVolume = normalizeKnockVolume(data.getDouble(key + ".knockVolume", DEFAULT_KNOCK_VOLUME));
    cache.put(uuid, new PlayerPref(enabled, doors, gates, trapdoors, autoClose, knockSound, knockVolume));
    } catch (IllegalArgumentException ignored) {
    // Non-UUID top-level key — skip silently.
    }
  }
  }

  /**
   * Saves all in-memory preferences synchronously to {@code players.yml}.
   * Performs blocking I/O.
   */
  public void save() {
  if (useSql) {
    flush();
    return;
  }

  try {
    saveYaml();
  } catch (IOException e) {
    plugin.getLogger().warning("Could not save players.yml: %s".formatted(e.getMessage()));
  }
  }

  private void saveYaml() throws IOException {
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
    data.set(key + ".knockVolume", pref.knockVolume());
  }
  data.save(dataFile);
  }

  /**
   * Saves asynchronously; safe to call from the main thread after every mutation.
   * Should be used from the main thread to avoid disk-blocking.
   */
  public void saveAsync(UUID changedUuid) {
  if (useSql) {
    if (changedUuid == null) {
    return;
    }
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
      pref.enableKnockSound(),
      pref.knockVolume());
    pendingSaves.put(changedUuid, new PendingSave(snapshot));
    scheduleWriter();
    return;
  }

  if (changedUuid == null) {
    return;
  }
  pendingSaves.put(changedUuid, PendingSave.forYaml());
  scheduleWriter();
  }

  /**
   * Flushes all pending async writes and blocks until persistence has completed.
   */
  public void flush() {
  if (writerExecutor.isShutdown()) {
    return;
  }
  try {
    writerExecutor.submit(this::drainPendingWrites).get();
  } catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    plugin.getLogger().warning("Interrupted while waiting for player preference writes to flush.");
  } catch (ExecutionException e) {
    plugin.getLogger().warning("Failed while flushing player preference writes: %s".formatted(e.getMessage()));
  }
  }

  /**
   * Flushes pending writes and terminates the player-preference writer thread.
   */
  public void close() {
  flush();
  writerExecutor.shutdown();
  try {
    if (!writerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
    writerExecutor.shutdownNow();
    // Wait a bit more for shutdownNow to complete
    if (!writerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
      plugin.getLogger().warning("Player preference writer did not terminate gracefully.");
    }
    }
  } catch (InterruptedException e) {
    writerExecutor.shutdownNow();
    Thread.currentThread().interrupt();
    plugin.getLogger().warning("Interrupted while shutting down player preference writer.");
  }
  }

  private void scheduleWriter() {
  if (writerExecutor.isShutdown()) {
    return;
  }
  if (!writerScheduled.compareAndSet(false, true)) {
    return;
  }
  writerExecutor.execute(this::drainPendingWrites);
  }

  private void drainPendingWrites() {
  boolean reschedule = true;
  try {
    while (true) {
    // Create a copy of the keys to iterate over without removing upfront
    Set<UUID> keysToProcess = Set.copyOf(pendingSaves.keySet());
    if (keysToProcess.isEmpty()) {
      return;
    }
    boolean failed = false;
    if (useSql) {
      for (UUID uuid : keysToProcess) {
      PendingSave pending = pendingSaves.get(uuid);
      if (pending == null || pending.sqlSnapshot() == null) {
        // Remove YAML entries from pendingSaves when using SQL
        pendingSaves.remove(uuid, pending);
        continue;
      }
      
      try {
        sqlStorage.savePlayerPreference(uuid, pending.sqlSnapshot());
        // Only remove after successful persistence
        pendingSaves.remove(uuid, pending);
      } catch (RuntimeException e) {
        failed = true;
        plugin.getLogger().warning("Could not save SQL player preference for %s: %s"
          .formatted(uuid, e.getMessage()));
      }
      }
    } else {
      // For YAML, we only save once per batch to avoid excessive I/O
      boolean yamlSaved = false;
      for (UUID uuid : keysToProcess) {
      PendingSave pending = pendingSaves.get(uuid);
      if (pending == null) {
        continue; // Entry was removed by another thread
      }
      
      // We only need to save once for YAML since it's a full file write
      if (!yamlSaved) {
        try {
        saveYaml();
        yamlSaved = true;
        // Remove all processed entries
        pendingSaves.keySet().removeAll(keysToProcess);
        } catch (IOException e) {
        failed = true;
        plugin.getLogger().warning("Could not save players.yml: %s".formatted(e.getMessage()));
        break;
        }
      }
      }
    }
    if (failed) {
      reschedule = false;
      return;
    }
    }
  } finally {
    writerScheduled.set(false);
    if (reschedule && !pendingSaves.isEmpty()) {
    scheduleWriter();
    }
  }
  }

  private PlayerPref getOrDefault(UUID uuid) {
  return cache.computeIfAbsent(uuid, k -> new PlayerPref(
    true,
    true,
    true,
    true,
    true,
    true,
    DEFAULT_KNOCK_VOLUME));
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

  /** Returns the player's knock sound volume (0.0-1.0). */
  public double getKnockVolume(UUID uuid) {
  return getOrDefault(uuid).knockVolume();
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
    current.enableKnockSound(),
    current.knockVolume()));
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
    current.enableKnockSound(),
    current.knockVolume()));
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
    current.enableKnockSound(),
    current.knockVolume()));
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
    current.enableKnockSound(),
    current.knockVolume()));
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
    current.enableKnockSound(),
    current.knockVolume()));
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
    next,
    current.knockVolume()));
  saveAsync(uuid);
  return next;
  }

  /**
   * Sets the player's knock volume preference.
   *
   * @param uuid the player's unique ID
   * @param volume volume between 0.0 and 1.0
   * @return the normalized volume that was stored
   */
  public double setKnockVolume(UUID uuid, double volume) {
  PlayerPref current = getOrDefault(uuid);
  double normalized = normalizeKnockVolume(volume);
  cache.put(uuid, new PlayerPref(
    current.enabled(),
    current.enableDoors(),
    current.enableFenceGates(),
    current.enableTrapdoors(),
    current.enableAutoClose(),
    current.enableKnockSound(),
    normalized));
  saveAsync(uuid);
  return normalized;
  }

  private static double normalizeKnockVolume(double value) {
  if (Double.isNaN(value) || Double.isInfinite(value)) {
    return DEFAULT_KNOCK_VOLUME;
  }
  if (value < MIN_KNOCK_VOLUME) {
    return MIN_KNOCK_VOLUME;
  }
  if (value > MAX_KNOCK_VOLUME) {
    return MAX_KNOCK_VOLUME;
  }
  return value;
  }

  /**
   * Immutable snapshot of a single player's preferences.
   *
   * @param enabled      global linked-door on/off
   * @param enableDoors    door-linking on/off
   * @param enableFenceGates fence-gate-linking on/off
   * @param enableTrapdoors  trapdoor-linking on/off
   * @param enableAutoClose  per-player auto-close on/off
   * @param enableKnockSound per-player knock sound on/off
   * @param knockVolume    per-player knock volume (0.0-1.0)
   */
  public record PlayerPref(
    boolean enabled,
    boolean enableDoors,
    boolean enableFenceGates,
    boolean enableTrapdoors,
    boolean enableAutoClose,
    boolean enableKnockSound,
    double knockVolume
  ) {}

  private static final class PendingSave {
  private final SharedSqlStorage.SqlPlayerPref sqlSnapshot;

  private PendingSave(SharedSqlStorage.SqlPlayerPref sqlSnapshot) {
    this.sqlSnapshot = sqlSnapshot;
  }

  private static PendingSave forYaml() {
    return new PendingSave(null);
  }

  private SharedSqlStorage.SqlPlayerPref sqlSnapshot() {
    return sqlSnapshot;
  }
  }

  private static final class WriterThreadFactory implements ThreadFactory {
  @Override
  public Thread newThread(Runnable runnable) {
    Thread thread = new Thread(runnable, "DoubleDoors-PlayerPrefWriter");
    thread.setDaemon(true);
    return thread;
  }
  }
}
