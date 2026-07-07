package me.szabee.doubledoors.i18n;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared translation resolver.
 */
public final class TranslationManager {

  private final TranslationCatalog catalog;
  private final String defaultLanguage;
  private final Map<String, Map<String, String>> translations;
  private volatile String activeLanguage;

  /**
   * Creates a translation manager.
   *
   * @param catalog         translation catalog providing language data
   * @param defaultLanguage fallback language code used when no translation is found
   */
  public TranslationManager(
    TranslationCatalog catalog,
    String defaultLanguage
  ) {
    this.catalog = catalog;
    this.defaultLanguage = defaultLanguage;
    this.translations = new ConcurrentHashMap<>();
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
    // ensure default is loaded (only cache non-empty loads so retries can work)
    if (!translations.containsKey(defaultLanguage)) {
      Map<String, String> defaults = catalog.load(defaultLanguage);
      if (!defaults.isEmpty()) {
        translations.put(defaultLanguage, defaults);
      }
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
      if (value != null && !value.isEmpty()) {
        return value;
      }
    }
    // fallback to active language
    Map<String, String> active = translations.get(activeLanguage);
    if (active != null && !languageCode.equals(activeLanguage)) {
      String value = active.get(key);
      if (value != null && !value.isEmpty()) {
        return value;
      }
    }
    // fallback to default language
    Map<String, String> def = translations.get(defaultLanguage);
    if (
      def != null &&
      !languageCode.equals(defaultLanguage) &&
      !activeLanguage.equals(defaultLanguage)
    ) {
      String value = def.get(key);
      if (value != null && !value.isEmpty()) {
        return value;
      }
    }
    return key;
  }
}
