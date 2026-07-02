package me.szabee.doubledoors.i18n;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.entity.Player;

import me.szabee.doubledoors.DoubleDoors;
import me.szabee.doubledoors.config.PlayerPreferences;

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
   */
  public void reload() {
    activeLanguage = plugin.getPluginConfig().getLanguage();
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
        ensureLoaded(playerLocale);
        String raw = lookup(playerLocale, key);
        return args.length == 0 ? raw : String.format(raw, args);
      }
    }
    return tr(key, args);
  }

  public String getActiveLanguage() {
    return activeLanguage;
  }

  /**
   * Returns all available language codes (bundled + custom).
   */
  public Set<String> getAvailableLanguages() {
    return TranslationCatalog.getAvailableLanguages(plugin);
  }

  private void ensureLoaded(String languageCode) {
    if (!translations.containsKey(languageCode)) {
      Map<String, String> loaded = TranslationCatalog.loadLanguageFile(plugin, languageCode);
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
