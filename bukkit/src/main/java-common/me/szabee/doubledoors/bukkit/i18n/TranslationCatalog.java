package me.szabee.doubledoors.bukkit.i18n;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Bukkit translation catalog adapter.
 */
public interface TranslationCatalog {
  /**
   * Language codes bundled in the plugin jar.
   */
  Set<String> BUNDLED_LANGUAGES = Set.of(
    "defaults", "en_US", "en_AU", "en_CA", "en_GB", "en_IE", "en_IN", "en_NZ", "en_ZA",
    "de_DE", "de_AT", "de_CH",
    "fr_FR", "fr_BE", "fr_CA", "fr_CH", "fr_CI", "fr_CM", "fr_DZ", "fr_GA", "fr_HT",
    "fr_LU", "fr_MC", "fr_MG", "fr_SN", "fr_TN",
    "es_ES", "es_409",
    "pt_PT", "pt_BR",
    "hu_HU",
    "uk_UA");

  /**
   * Lists all available language codes: bundled plus any custom files in the data folder.
   */
  static Set<String> getAvailableLanguages(JavaPlugin plugin) {
    Set<String> codes = new HashSet<>(BUNDLED_LANGUAGES);
    codes.remove("defaults");
    File langDir = new File(plugin.getDataFolder(), "lang");
    if (langDir.isDirectory()) {
      File[] files = langDir.listFiles((dir, name) -> name.endsWith(".json"));
      if (files != null) {
        for (File f : files) {
          String name = f.getName();
          codes.add(name.substring(0, name.length() - 5));
        }
      }
    }
    return codes;
  }

  Map<String, String> load(String languageCode);

  /**
   * Loads a language file from plugin resources (bundled) or data folder (overrides).
   * Bundled files are in {@code lang/&lt;code&gt;.json} inside the jar.
   * Custom files can be placed in {@code plugins/DoubleDoors/lang/&lt;code&gt;.json}.
   *
   * @param plugin the plugin instance
   * @param languageCode language code
   * @return translations map
   */
  static Map<String, String> loadLanguageFile(JavaPlugin plugin, String languageCode) {
    // Check data folder first (user overrides)
    File dataFile = new File(plugin.getDataFolder(), "lang" + File.separator + languageCode + ".json");
    if (dataFile.isFile()) {
      try (InputStream in = new FileInputStream(dataFile)) {
        return parseJson(in);
      } catch (IOException ignored) {}
    }

    // Fall back to bundled resources
    String resourcePath = "lang/" + languageCode + ".json";
    try (InputStream in = plugin.getResource(resourcePath)) {
      if (in != null) {
        return parseJson(in);
      }
    } catch (IOException ignored) {}

    return Map.of();
  }

  /**
   * Loads locale contributor credits from {@code lang/credits.json}.
   * Custom credits in the plugin data folder override bundled credits when present.
   *
   * @param plugin the plugin instance
   * @return a map of language code to contributor names
   */
  static Map<String, List<String>> loadLanguageCredits(JavaPlugin plugin) {
    if (plugin == null) {
      return Map.of();
    }

    File dataFile = new File(plugin.getDataFolder(), "lang" + File.separator + "credits.json");
    if (dataFile.isFile()) {
      try (InputStream in = new FileInputStream(dataFile)) {
        return parseCreditsJson(in);
      } catch (IOException ignored) {}
    }

    try (InputStream in = plugin.getResource("lang/credits.json")) {
      if (in != null) {
        return parseCreditsJson(in);
      }
    } catch (IOException ignored) {}

    return Map.of();
  }

  private static Map<String, String> parseJson(InputStream in) throws IOException {
    try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
      Map<String, Object> raw = new Gson().fromJson(reader, new TypeToken<Map<String, Object>>() {}.getType());
      Map<String, String> result = new java.util.HashMap<>();
      if (raw != null) {
        for (var entry : raw.entrySet()) {
          if (entry.getValue() instanceof String s) {
            result.put(entry.getKey(), s);
          }
        }
      }
      return result;
    }
  }

  private static Map<String, List<String>> parseCreditsJson(InputStream in) throws IOException {
    try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
      Map<String, List<String>> raw = new Gson().fromJson(reader, new TypeToken<Map<String, List<String>>>() {}.getType());
      return raw == null ? Map.of() : raw;
    }
  }
}
