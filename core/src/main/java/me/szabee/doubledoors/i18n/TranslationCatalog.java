package me.szabee.doubledoors.i18n;

import java.util.Map;

/**
 * Loader-neutral translation catalog.
 */
public interface TranslationCatalog {
  /**
   * Returns all translations for a language.
   *
   * @param languageCode language code
   * @return translations
   */
  Map<String, String> load(String languageCode);
}
