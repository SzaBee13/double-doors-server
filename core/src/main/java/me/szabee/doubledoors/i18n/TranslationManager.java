package me.szabee.doubledoors.i18n;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Shared translation resolver.
 */
public final class TranslationManager {
  private final TranslationCatalog catalog;
  private final String defaultLanguage;
  private final Map<String, Map<String, String>> translations;
  private String activeLanguage;

  public TranslationManager(TranslationCatalog catalog, String defaultLanguage) {
    this.catalog = catalog;
    this.defaultLanguage = defaultLanguage;
    this.translations = new HashMap<>();
    this.activeLanguage = defaultLanguage;
  }

  /**
   * Reloads (or loads) translations for the given language code, caching them internally.
   * The server's active language is NOT changed; use {@link #setActiveLanguage} for that.
   */
  public void reload(String languageCode) {
    Map<String, String> loaded = catalog.load(languageCode);
    if (!loaded.isEmpty()) {
      translations.put(languageCode, loaded);
    }
    // ensure default is loaded
    if (!translations.containsKey(defaultLanguage)) {
      Map<String, String> defaults = catalog.load(defaultLanguage);
      translations.put(defaultLanguage, defaults);
    }
  }

  /**
   * Sets the active server language (used by {@link #tr(String, Object...)}).
   */
  public void setActiveLanguage(String languageCode) {
    this.activeLanguage = languageCode;
  }

  /**
   * Gets the active server language code.
   */
  public String getActiveLanguage() {
    return activeLanguage;
  }

  /**
   * Returns all available language codes that have been loaded so far.
   */
  public Set<String> getAvailableLanguages() {
    return translations.keySet();
  }

  /**
   * Translates {@code key} using the active server language.
   */
  public String tr(String key, Object... args) {
    String raw = lookup(activeLanguage, key);
    return args.length == 0 ? raw : String.format(raw, args);
  }

  /**
   * Translates {@code key} using the given {@code languageCode}.
   * Falls back to the server's active language if the given code has no translations.
   */
  public String tr(String languageCode, String key, Object... args) {
    String raw = lookup(languageCode, key);
    return args.length == 0 ? raw : String.format(raw, args);
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
    // fallback to default language
    Map<String, String> def = translations.get(defaultLanguage);
    if (def != null && !languageCode.equals(defaultLanguage) && !activeLanguage.equals(defaultLanguage)) {
      String value = def.get(key);
      if (value != null) {
        return value;
      }
    }
    return key;
  }
}
