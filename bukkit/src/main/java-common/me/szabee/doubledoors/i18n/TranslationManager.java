package me.szabee.doubledoors.i18n;

import java.util.Map;

/**
 * Bukkit translation manager adapter.
 */
public final class TranslationManager {
  private final me.szabee.doubledoors.i18n.TranslationManager delegate;
  private String activeLanguage;

  public TranslationManager(TranslationCatalog catalog, String defaultLanguage) {
    this.delegate = new me.szabee.doubledoors.i18n.TranslationManager(catalog::load, defaultLanguage);
    this.activeLanguage = defaultLanguage;
  }

  public void reload(String languageCode) {
    delegate.reload(languageCode);
    activeLanguage = languageCode;
  }

  public String tr(String key, Object... args) {
    return delegate.tr(key, args);
  }

  public String getActiveLanguage() {
    return activeLanguage;
  }

  public String tr(org.bukkit.entity.Player player, String key, Object... args) {
    return tr(key, args);
  }
}
