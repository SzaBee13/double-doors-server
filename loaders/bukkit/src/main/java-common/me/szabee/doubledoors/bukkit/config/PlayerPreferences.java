package me.szabee.doubledoors.bukkit.config;

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
import me.szabee.doubledoors.bukkit.DoubleDoors;
import me.szabee.doubledoors.bukkit.i18n.TranslationCatalog;
import me.szabee.doubledoors.storage.SharedSqlStorage;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Manages per-player preferences, persisted to {@code players.yml} inside the plugin data folder.
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
  private final AtomicBoolean writerScheduled = new AtomicBoolean(false);
  private final ExecutorService writerExecutor =
    Executors.newSingleThreadExecutor(new WriterThreadFactory());

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

  /** Reloads all preferences from disk, clearing the in-memory cache. */
  public void load() {
    cache.clear();
    if (useSql) {
      for (Map.Entry<UUID, SharedSqlStorage.SqlPlayerPref> entry : sqlStorage
        .loadAllPlayerPreferences()
        .entrySet()) {
        SharedSqlStorage.SqlPlayerPref pref = entry.getValue();
        cache.put(
          entry.getKey(),
          new PlayerPref(
            pref.enabled(),
            pref.enableDoors(),
            pref.enableFenceGates(),
            pref.enableTrapdoors(),
            pref.enableAutoClose(),
            pref.enableKnockSound(),
            normalizeKnockVolume(pref.knockVolume()),
            normalizeLocale(pref.locale())
          )
        );
      }
      return;
    }

    YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
    for (String key : data.getKeys(false)) {
      if (
        key.length() != 36 ||
        key.charAt(8) != '-' ||
        key.charAt(13) != '-' ||
        key.charAt(18) != '-' ||
        key.charAt(23) != '-'
      ) {
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
        double knockVolume = normalizeKnockVolume(
          data.getDouble(key + ".knockVolume", DEFAULT_KNOCK_VOLUME)
        );
        String locale = normalizeLocale(data.getString(key + ".locale", ""));
        cache.put(
          uuid,
          new PlayerPref(
            enabled,
            doors,
            gates,
            trapdoors,
            autoClose,
            knockSound,
            knockVolume,
            locale
          )
        );
      } catch (IllegalArgumentException e) {
        plugin
          .getLogger()
          .fine(
            "Skipping malformed player entry in players.yml: " + e.getMessage()
          );
      }
    }
  }

  /** Saves all in-memory preferences synchronously to {@code players.yml}. */
  public void save() {
    if (useSql) {
      flush();
      return;
    }

    try {
      saveYaml();
    } catch (IOException e) {
      plugin
        .getLogger()
        .warning("Could not save players.yml: %s".formatted(e.getMessage()));
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
      data.set(key + ".locale", pref.locale());
    }
    data.save(dataFile);
  }

  /** Saves asynchronously; safe to call from the main thread after every mutation. */
  public void saveAsync(UUID changedUuid) {
    if (changedUuid == null) {
      return;
    }
    if (useSql) {
      PlayerPref pref = cache.get(changedUuid);
      if (pref == null) {
        return;
      }
      pendingSaves.put(
        changedUuid,
        new PendingSave(
          new SharedSqlStorage.SqlPlayerPref(
            pref.enabled(),
            pref.enableDoors(),
            pref.enableFenceGates(),
            pref.enableTrapdoors(),
            pref.enableAutoClose(),
            pref.enableKnockSound(),
            pref.knockVolume(),
            pref.locale()
          )
        )
      );
      scheduleWriter();
      return;
    }
    pendingSaves.put(changedUuid, PendingSave.forYaml());
    scheduleWriter();
  }

  /** Flushes all pending async writes and blocks until persistence has completed. */
  public void flush() {
    if (writerExecutor.isShutdown()) {
      return;
    }
    try {
      writerExecutor.submit(this::drainPendingWrites).get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      plugin
        .getLogger()
        .warning(
          "Interrupted while waiting for player preference writes to flush."
        );
    } catch (ExecutionException e) {
      plugin
        .getLogger()
        .warning(
          "Failed while flushing player preference writes: %s".formatted(
            e.getMessage()
          )
        );
    }
  }

  /** Flushes pending writes and terminates the player-preference writer thread. */
  public void close() {
    flush();
    writerExecutor.shutdown();
    try {
      if (!writerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
        writerExecutor.shutdownNow();
        if (!writerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
          plugin
            .getLogger()
            .warning("Player preference writer did not terminate gracefully.");
        }
      }
    } catch (InterruptedException e) {
      writerExecutor.shutdownNow();
      Thread.currentThread().interrupt();
      plugin
        .getLogger()
        .warning("Interrupted while shutting down player preference writer.");
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
    try {
      while (true) {
        Set<UUID> keysToProcess = Set.copyOf(pendingSaves.keySet());
        if (keysToProcess.isEmpty()) {
          return;
        }
        boolean failed = false;
        if (useSql) {
          for (UUID uuid : keysToProcess) {
            PendingSave pending = pendingSaves.get(uuid);
            if (pending == null || pending.sqlSnapshot() == null) {
              pendingSaves.remove(uuid, pending);
              continue;
            }
            try {
              boolean saved = sqlStorage.savePlayerPreference(
                uuid,
                pending.sqlSnapshot()
              );
              if (saved) {
                pendingSaves.remove(uuid, pending);
              } else {
                failed = true;
                plugin
                  .getLogger()
                  .warning(
                    "Could not save SQL player preference for %s: storage returned false".formatted(
                      uuid
                    )
                  );
              }
            } catch (RuntimeException e) {
              failed = true;
              plugin
                .getLogger()
                .warning(
                  "Could not save SQL player preference for %s: %s".formatted(
                    uuid,
                    e.getMessage()
                  )
                );
            }
          }
        } else {
          try {
            Map<UUID, PendingSave> batch = new java.util.HashMap<>();
            for (UUID uuid : keysToProcess) {
              PendingSave pending = pendingSaves.get(uuid);
              if (pending != null) {
                batch.put(uuid, pending);
              }
            }
            saveYaml();
            for (Map.Entry<UUID, PendingSave> entry : batch.entrySet()) {
              pendingSaves.remove(entry.getKey(), entry.getValue());
            }
          } catch (IOException e) {
            failed = true;
            plugin
              .getLogger()
              .warning(
                "Could not save players.yml: %s".formatted(e.getMessage())
              );
          }
        }
        if (failed) {
          return;
        }
      }
    } finally {
      writerScheduled.set(false);
      if (!pendingSaves.isEmpty()) {
        scheduleWriter();
      }
    }
  }

  private PlayerPref getOrDefault(UUID uuid) {
    return cache.computeIfAbsent(uuid, k ->
      new PlayerPref(
        true,
        true,
        true,
        true,
        true,
        true,
        DEFAULT_KNOCK_VOLUME,
        ""
      )
    );
  }

  /**
   * Returns whether the player has not globally disabled linked-door behavior.
   *
   * @param uuid the player's UUID
   */
  public boolean isEnabled(UUID uuid) {
    return getOrDefault(uuid).enabled();
  }

  /**
   * Returns whether the player has doors linking enabled.
   *
   * @param uuid the player's UUID
   */
  public boolean isDoorsEnabled(UUID uuid) {
    return getOrDefault(uuid).enableDoors();
  }

  /**
   * Returns whether the player has fence-gate linking enabled.
   *
   * @param uuid the player's UUID
   */
  public boolean isFenceGatesEnabled(UUID uuid) {
    return getOrDefault(uuid).enableFenceGates();
  }

  /**
   * Returns whether the player has trapdoor linking enabled.
   *
   * @param uuid the player's UUID
   */
  public boolean isTrapdoorsEnabled(UUID uuid) {
    return getOrDefault(uuid).enableTrapdoors();
  }

  /**
   * Returns whether the player has auto-close enabled for their interactions.
   *
   * @param uuid the player's UUID
   */
  public boolean isAutoCloseEnabled(UUID uuid) {
    return getOrDefault(uuid).enableAutoClose();
  }

  /**
   * Returns whether the player has knock sound enabled for their interactions.
   *
   * @param uuid the player's UUID
   */
  public boolean isKnockSoundEnabled(UUID uuid) {
    return getOrDefault(uuid).enableKnockSound();
  }

  /**
   * Returns the player's knock sound volume (0.0-1.0).
   *
   * @param uuid the player's UUID
   */
  public double getKnockVolume(UUID uuid) {
    return getOrDefault(uuid).knockVolume();
  }

  /**
   * Returns the player's preferred locale override, or blank when unset.
   *
   * @param uuid the player's UUID
   */
  public String getLocale(UUID uuid) {
    return getOrDefault(uuid).locale();
  }

  /**
   * Sets the player's preferred locale override.
   *
   * @param uuid   the player's UUID
   * @param locale the new locale code
   * @return the normalized locale string
   */
  public String setLocale(UUID uuid, String locale) {
    PlayerPref current = getOrDefault(uuid);
    String normalized = normalizeLocale(locale);
    cache.put(
      uuid,
      new PlayerPref(
        current.enabled(),
        current.enableDoors(),
        current.enableFenceGates(),
        current.enableTrapdoors(),
        current.enableAutoClose(),
        current.enableKnockSound(),
        current.knockVolume(),
        normalized
      )
    );
    saveAsync(uuid);
    return normalized;
  }

  /**
   * Toggles the player's global linked-door on/off switch.
   *
   * @param uuid the player's UUID
   * @return the new state
   */
  public boolean toggleAll(UUID uuid) {
    PlayerPref current = getOrDefault(uuid);
    boolean next = !current.enabled();
    cache.put(
      uuid,
      new PlayerPref(
        next,
        current.enableDoors(),
        current.enableFenceGates(),
        current.enableTrapdoors(),
        current.enableAutoClose(),
        current.enableKnockSound(),
        current.knockVolume(),
        current.locale()
      )
    );
    saveAsync(uuid);
    return next;
  }

  /**
   * Toggles the door-linking preference for the given player.
   *
   * @param uuid the player's UUID
   * @return the new state
   */
  public boolean toggleDoors(UUID uuid) {
    PlayerPref current = getOrDefault(uuid);
    boolean next = !current.enableDoors();
    cache.put(
      uuid,
      new PlayerPref(
        current.enabled(),
        next,
        current.enableFenceGates(),
        current.enableTrapdoors(),
        current.enableAutoClose(),
        current.enableKnockSound(),
        current.knockVolume(),
        current.locale()
      )
    );
    saveAsync(uuid);
    return next;
  }

  /**
   * Toggles the fence-gate-linking preference for the given player.
   *
   * @param uuid the player's UUID
   * @return the new state
   */
  public boolean toggleFenceGates(UUID uuid) {
    PlayerPref current = getOrDefault(uuid);
    boolean next = !current.enableFenceGates();
    cache.put(
      uuid,
      new PlayerPref(
        current.enabled(),
        current.enableDoors(),
        next,
        current.enableTrapdoors(),
        current.enableAutoClose(),
        current.enableKnockSound(),
        current.knockVolume(),
        current.locale()
      )
    );
    saveAsync(uuid);
    return next;
  }

  /**
   * Toggles the trapdoor-linking preference for the given player.
   *
   * @param uuid the player's UUID
   * @return the new state
   */
  public boolean toggleTrapdoors(UUID uuid) {
    PlayerPref current = getOrDefault(uuid);
    boolean next = !current.enableTrapdoors();
    cache.put(
      uuid,
      new PlayerPref(
        current.enabled(),
        current.enableDoors(),
        current.enableFenceGates(),
        next,
        current.enableAutoClose(),
        current.enableKnockSound(),
        current.knockVolume(),
        current.locale()
      )
    );
    saveAsync(uuid);
    return next;
  }

  /**
   * Toggles auto-close preference for the given player.
   *
   * @param uuid the player's UUID
   * @return the new state
   */
  public boolean toggleAutoClose(UUID uuid) {
    PlayerPref current = getOrDefault(uuid);
    boolean next = !current.enableAutoClose();
    cache.put(
      uuid,
      new PlayerPref(
        current.enabled(),
        current.enableDoors(),
        current.enableFenceGates(),
        current.enableTrapdoors(),
        next,
        current.enableKnockSound(),
        current.knockVolume(),
        current.locale()
      )
    );
    saveAsync(uuid);
    return next;
  }

  /**
   * Toggles knock-sound preference for the given player.
   *
   * @param uuid the player's UUID
   * @return the new state
   */
  public boolean toggleKnockSound(UUID uuid) {
    PlayerPref current = getOrDefault(uuid);
    boolean next = !current.enableKnockSound();
    cache.put(
      uuid,
      new PlayerPref(
        current.enabled(),
        current.enableDoors(),
        current.enableFenceGates(),
        current.enableTrapdoors(),
        current.enableAutoClose(),
        next,
        current.knockVolume(),
        current.locale()
      )
    );
    saveAsync(uuid);
    return next;
  }

  /**
   * Sets the player's knock volume preference.
   *
   * @param uuid   the player's UUID
   * @param volume the new volume (clamped to 0.0-1.0)
   * @return the normalized volume
   */
  public double setKnockVolume(UUID uuid, double volume) {
    PlayerPref current = getOrDefault(uuid);
    double normalized = normalizeKnockVolume(volume);
    cache.put(
      uuid,
      new PlayerPref(
        current.enabled(),
        current.enableDoors(),
        current.enableFenceGates(),
        current.enableTrapdoors(),
        current.enableAutoClose(),
        current.enableKnockSound(),
        normalized,
        current.locale()
      )
    );
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

  private String normalizeLocale(String locale) {
    if (locale == null) {
      return "";
    }
    String trimmed = locale.trim();
    if (trimmed.length() > 32) {
      trimmed = trimmed.substring(0, 32);
    }
    if (trimmed.isEmpty()) {
      return trimmed;
    }
    return TranslationCatalog.resolveLanguageCode(plugin, trimmed);
  }

  /** Immutable snapshot of a single player's preferences. */
  public record PlayerPref(
    boolean enabled,
    boolean enableDoors,
    boolean enableFenceGates,
    boolean enableTrapdoors,
    boolean enableAutoClose,
    boolean enableKnockSound,
    double knockVolume,
    String locale
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
