package me.szabee.doubledoors.bukkit.i18n;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.szabee.doubledoors.bukkit.DoubleDoors;
import me.szabee.doubledoors.bukkit.config.PlayerPreferences;
import org.bukkit.entity.Player;

/**
 * Bukkit translation manager.
 * <p>
 * Use {@link #tr(String, Object...)} for console/server-language messages.
 * Use {@link #tr(Player, String, Object...)} for player-specific messages,
 * which respects per-player locale overrides (if enabled).
 */
public final class TranslationManager {

  private final DoubleDoors plugin;
  private final Map<String, Map<String, String>> translations;
  private String activeLanguage;

  /**
   * Creates a translation manager bound to the given plugin instance.
   * The server's configured language is loaded eagerly.
   *
   * @param plugin the plugin instance
   */
  public TranslationManager(DoubleDoors plugin) {
    this.plugin = plugin;
    this.translations = new HashMap<>();
    this.activeLanguage = plugin.getPluginConfig().getLanguage();
    ensureLoaded(activeLanguage);
  }

  // Package-private for unit testing
  TranslationManager() {
    this.plugin = null;
    this.translations = new HashMap<>();
    this.activeLanguage = "en_US";
  }

  /**
   * (Re)loads translations for the server's configured language.
   * Also invalidates cached defaults so that {@code defaults.json}
   * is re-read on next access.
   */
  public void reload() {
    TranslationCatalog.invalidateDefaults();
    activeLanguage = plugin.getPluginConfig().getLanguage();
    translations.remove(activeLanguage);
    ensureLoaded(activeLanguage);
  }

  /**
   * Translates using the server's configured language (for console/log messages).
   */
  public String tr(String key, Object... args) {
    String raw = lookup(activeLanguage, key);
    return args.length == 0 ? raw : String.format(raw, args);
  }

  /**
   * Translates using the player's preferred locale if one is set,
   * falling back to the server's language otherwise.
   */
  public String tr(Player player, String key, Object... args) {
    if (player == null) {
      return tr(key, args);
    }
    PlayerPreferences prefs = plugin.getPlayerPreferences();
    if (prefs != null) {
      String playerLocale = prefs.getLocale(player.getUniqueId());
      if (!playerLocale.isBlank()) {
        String resolved = TranslationCatalog.resolveLanguageCode(
          plugin,
          playerLocale
        );
        if (!resolved.isBlank()) {
          ensureLoaded(resolved);
          String raw = lookup(resolved, key);
          return args.length == 0 ? raw : String.format(raw, args);
        }
      }
    }
    return tr(key, args);
  }

  /**
   * Returns the server's currently active language code.
   *
   * @return the active language code (e.g. {@code "en_US"})
   */
  public String getActiveLanguage() {
    return activeLanguage;
  }

  /**
   * Returns all available language codes (bundled + custom).
   */
  public Set<String> getAvailableLanguages() {
    return TranslationCatalog.getAvailableLanguages(plugin);
  }

  /**
   * Returns the contributor credits for a language code.
   *
   * @param languageCode the language code to inspect
   * @return contributor names, or an empty list when no credits are defined
   */
  public List<String> getLanguageCredits(String languageCode) {
    if (plugin == null || languageCode == null || languageCode.isBlank()) {
      return List.of();
    }
    return TranslationCatalog.getLanguageCredits(plugin, languageCode);
  }

  /**
   * Returns the display name for a language code from {@code defaults.json},
   * falling back to the language code itself if not found.
   */
  public String getLanguageName(String languageCode) {
    if (plugin == null || languageCode == null || languageCode.isBlank()) {
      return languageCode;
    }
    return TranslationCatalog.getLanguageName(plugin, languageCode);
  }

  /**
   * Calculates the translation completion percentage for a language.
   *
   * @param languageCode the canonical language code
   * @return completion percentage between 0.0 and 100.0
   */
  public double getCompletionPercentage(String languageCode) {
    if (plugin == null || languageCode == null || languageCode.isBlank()) {
      return 0.0;
    }
    return TranslationCatalog.getCompletionPercentage(plugin, languageCode);
  }

  /**
   * Resolves a user-supplied language input to a canonical code.
   *
   * @param input the user-supplied language code or alias
   * @return the canonical code, or the original input if no alias matches
   */
  public String resolveLanguageCode(String input) {
    if (plugin == null || input == null || input.isBlank()) {
      return input;
    }
    return TranslationCatalog.resolveLanguageCode(plugin, input);
  }

  private void ensureLoaded(String languageCode) {
    if (!translations.containsKey(languageCode)) {
      Map<String, String> loaded = TranslationCatalog.loadLanguageFile(
        plugin,
        languageCode
      );
      translations.put(languageCode, loaded);
    }
  }

  private String lookup(String languageCode, String key) {
    Map<String, String> map = translations.get(languageCode);
    if (map != null) {
      String value = map.get(key);
      if (value != null) {
        return value;
      }
    }
    // fallback to active language
    Map<String, String> active = translations.get(activeLanguage);
    if (active != null && !languageCode.equals(activeLanguage)) {
      String value = active.get(key);
      if (value != null) {
        return value;
      }
    }
    // fallback to first loaded language
    for (Map<String, String> m : translations.values()) {
      String value = m.get(key);
      if (value != null) {
        return value;
      }
    }
    return key;
  }
}
