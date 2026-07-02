package me.szabee.doubledoors.i18n;

import java.util.Map;

/**
 * Bukkit translation catalog adapter.
 */
public interface TranslationCatalog {
  Map<String, String> load(String languageCode);
}
