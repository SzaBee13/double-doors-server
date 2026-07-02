package me.szabee.doubledoors.i18n;

import java.util.Map;

/**
 * Shared translation resolver.
 */
public final class TranslationManager {
  private final TranslationCatalog catalog;
  private final String defaultLanguage;
  private Map<String, String> activeTranslations;

  public TranslationManager(TranslationCatalog catalog, String defaultLanguage) {
    this.catalog = catalog;
    this.defaultLanguage = defaultLanguage;
    this.activeTranslations = Map.of();
  }

  public void reload(String languageCode) {
    Map<String, String> defaults = catalog.load(defaultLanguage);
    Map<String, String> requested = catalog.load(languageCode);
    this.activeTranslations = requested.isEmpty() ? defaults : requested;
  }

  public String tr(String key, Object... args) {
    String raw = activeTranslations.getOrDefault(key, key);
    return args.length == 0 ? raw : String.format(raw, args);
  }
}
