package me.szabee.doubledoors.bukkit.i18n;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Bukkit translation catalog adapter.
 * <p>
 * Language metadata (names, credits, aliases) and default fallback strings are
 * sourced from {@code lang/defaults.json}. Individual locale files contain only
 * translated strings.
 */
public final class TranslationCatalog {

  private TranslationCatalog() {}

  /**
   * Parsed snapshot of {@code lang/defaults.json}.
   *
   * @param strings      default fallback translation strings
   * @param languageNames canonical code to display name
   * @param credits      canonical code to contributor list
   * @param aliasToCode  lowercased alias to canonical code
   * @param canonicalCodes all canonical language codes defined in defaults.json
   */
  public record DefaultsData(
    Map<String, String> strings,
    Map<String, String> languageNames,
    Map<String, List<String>> credits,
    Map<String, String> aliasToCode,
    Set<String> canonicalCodes
  ) {}

  private static DefaultsData cachedDefaults;

  /**
   * Returns the parsed {@code defaults.json} data, loading and caching it on
   * first access. Call {@link #invalidateDefaults()} to force a reload.
   *
   * @param plugin the plugin instance
   * @return the parsed defaults data
   */
  static DefaultsData loadDefaults(JavaPlugin plugin) {
    DefaultsData data = cachedDefaults;
    if (data != null) {
      return data;
    }
    data = loadDefaultsFromPlugin(plugin);
    cachedDefaults = data;
    return data;
  }

  /**
   * Clears the cached defaults data so the next {@link #loadDefaults} call
   * re-reads from disk.
   */
  static void invalidateDefaults() {
    cachedDefaults = null;
  }

  /**
   * Resolves a user-supplied language input to a canonical code using the
   * alias map from {@code defaults.json}. Matching is case-insensitive.
   *
   * @param plugin the plugin instance
   * @param input  the user-supplied language code or alias
   * @return the canonical code, or the original input if no alias matches
   */
  public static String resolveLanguageCode(JavaPlugin plugin, String input) {
    if (input == null || input.isBlank()) {
      return "";
    }
    String trimmed = input.trim();
    if ("custom".equalsIgnoreCase(trimmed)) {
      return "custom";
    }
    DefaultsData defaults = loadDefaults(plugin);
    String canonical = defaults
      .aliasToCode()
      .get(trimmed.toLowerCase(Locale.ROOT));
    return canonical != null ? canonical : trimmed;
  }

  /**
   * Lists all available language codes: those defined in {@code defaults.json}
   * plus any custom files in the data folder.
   *
   * @param plugin the plugin instance
   * @return set of canonical language codes
   */
  static Set<String> getAvailableLanguages(JavaPlugin plugin) {
    DefaultsData defaults = loadDefaults(plugin);
    Set<String> codes = new HashSet<>(defaults.canonicalCodes());

    if (isCustomLanguageFilePresent(plugin)) {
      codes.add("custom");
    }

    File langDir = new File(plugin.getDataFolder(), "lang");
    if (langDir.isDirectory()) {
      File[] files = langDir.listFiles((dir, name) ->
        name.endsWith(".json")
      );
      if (files != null) {
        for (File f : files) {
          String name = f.getName();
          String code = name.substring(0, name.length() - 5);
          if (!"defaults".equals(code)) {
            codes.add(code);
          }
        }
      }
    }
    return codes;
  }

  /**
   * Checks whether the custom translation file exists in the data folder.
   */
  public static boolean isCustomLanguageFilePresent(JavaPlugin plugin) {
    return new File(plugin.getDataFolder(), "custom_lang.json").isFile();
  }

  /**
   * Returns the display name for a language code from {@code defaults.json},
   * falling back to the code itself when not found.
   *
   * @param plugin       the plugin instance
   * @param languageCode the canonical language code
   * @return the display name
   */
  static String getLanguageName(JavaPlugin plugin, String languageCode) {
    if ("custom".equalsIgnoreCase(languageCode)) {
      return "Custom";
    }
    DefaultsData defaults = loadDefaults(plugin);
    String name = defaults.languageNames().get(languageCode);
    return name != null ? name : languageCode;
  }

  /**
   * Returns the contributor credits for a language code from
   * {@code defaults.json}.
   *
   * @param plugin       the plugin instance
   * @param languageCode the canonical language code
   * @return contributor names, or an empty list when no credits are defined
   */
  static List<String> getLanguageCredits(
    JavaPlugin plugin,
    String languageCode
  ) {
    DefaultsData defaults = loadDefaults(plugin);
    List<String> c = defaults.credits().get(languageCode);
    return c != null ? List.copyOf(c) : List.of();
  }

  /**
   * Calculates the translation completion percentage for a language by
   * comparing its keys against the default strings in {@code defaults.json}.
   *
   * @param plugin       the plugin instance
   * @param languageCode the canonical language code
   * @return completion percentage between 0.0 and 100.0
   */
  static double getCompletionPercentage(
    JavaPlugin plugin,
    String languageCode
  ) {
    DefaultsData defaults = loadDefaults(plugin);
    Map<String, String> referenceStrings = defaults.strings();
    if (referenceStrings.isEmpty()) {
      return 100.0;
    }

    Map<String, String> langStrings;
    if ("custom".equalsIgnoreCase(languageCode)) {
      langStrings = loadCustomLanguageFileRaw(plugin);
    } else {
      langStrings = loadLanguageFileRaw(plugin, languageCode);
    }
    int total = referenceStrings.size();
    if (total == 0) {
      return 100.0;
    }

    int translated = 0;
    for (var entry : referenceStrings.entrySet()) {
      String value = langStrings.get(entry.getKey());
      if (value != null && !value.isBlank()) {
        translated++;
      }
    }
    return (translated * 100.0) / total;
  }

  /**
   * Loads translations for the given language code from bundled resources or
   * the data folder. Strings from {@code defaults.json/strings} serve as
   * fallback when a key is missing in the locale file.
   *
   * @param plugin       the plugin instance
   * @param languageCode the language code to load (e.g. {@code "en_US"})
   * @return a map of translation keys to localized strings
   */
  public static Map<String, String> loadLanguageFile(
    JavaPlugin plugin,
    String languageCode
  ) {
    Map<String, String> result = loadLanguageFileRaw(plugin, languageCode);

    DefaultsData defaults = loadDefaults(plugin);
    if (!defaults.strings().isEmpty()) {
      Map<String, String> merged = new HashMap<>(defaults.strings());
      merged.putAll(result);
      return merged;
    }
    return result;
  }

  /**
   * Loads the custom translation file ({@code custom_lang.json}) from the
   * plugin data folder. If the file does not exist, the bundled
   * {@code lang/en_US.json} is copied there as a starting template.
   * <p>
   * The returned map has {@code defaults.json} strings as the base, overlaid
   * with the custom file's values.
   *
   * @param plugin the plugin instance
   * @return the merged translation map
   */
  public static Map<String, String> loadCustomLanguageFile(JavaPlugin plugin) {
    File customFile = new File(
      plugin.getDataFolder(),
      "custom_lang.json"
    );
    if (!customFile.isFile()) {
      writeDefaultCustomFile(plugin, customFile);
    }
    Map<String, String> result = Map.of();
    if (customFile.isFile()) {
      try (InputStream in = new FileInputStream(customFile)) {
        result = parseJson(in);
      } catch (IOException | JsonSyntaxException e) {
        plugin
          .getLogger()
          .log(Level.WARNING, "Failed to parse custom_lang.json, using defaults", e);
      }
    }
    DefaultsData defaults = loadDefaults(plugin);
    if (!defaults.strings().isEmpty()) {
      Map<String, String> merged = new HashMap<>(defaults.strings());
      merged.putAll(result);
      return merged;
    }
    return result;
  }

  /**
   * Loads only the raw entries from {@code custom_lang.json} without merging
   * defaults. Returns only the strings defined in the custom file itself.
   *
   * @param plugin the plugin instance
   * @return the raw custom translation map
   */
  private static Map<String, String> loadCustomLanguageFileRaw(
    JavaPlugin plugin
  ) {
    File customFile = new File(
      plugin.getDataFolder(),
      "custom_lang.json"
    );
    if (!customFile.isFile()) {
      return Map.of();
    }
    try (InputStream in = new FileInputStream(customFile)) {
      return parseJson(in);
    } catch (IOException | JsonSyntaxException e) {
      plugin
        .getLogger()
        .log(Level.WARNING, "Failed to parse custom_lang.json", e);
      return Map.of();
    }
  }

  private static void writeDefaultCustomFile(
    JavaPlugin plugin,
    File customFile
  ) {
    try (InputStream in = plugin.getResource("lang/en_US.json")) {
      if (in == null) {
        return;
      }
      Files.createDirectories(customFile.getParentFile().toPath());
      Files.copy(in, customFile.toPath());
    } catch (IOException ignored) {}
  }

  /**
   * Loads a language file from plugin resources (bundled) or data folder
   * (overrides) without merging defaults. Returns only the strings defined
   * in the locale file itself.
   */
  private static Map<String, String> loadLanguageFileRaw(
    JavaPlugin plugin,
    String languageCode
  ) {
    if (!isSafeLanguageCode(languageCode)) {
      return Map.of();
    }

    File dataFile = new File(
      plugin.getDataFolder(),
      "lang" + File.separator + languageCode + ".json"
    );
    if (dataFile.isFile()) {
      try (InputStream in = new FileInputStream(dataFile)) {
        return parseJson(in);
      } catch (IOException ignored) {}
    }

    String resourcePath = "lang/" + languageCode + ".json";
    try (InputStream in = plugin.getResource(resourcePath)) {
      if (in != null) {
        return parseJson(in);
      }
    } catch (IOException ignored) {}

    return Map.of();
  }

  private static boolean isSafeLanguageCode(String languageCode) {
    if (languageCode == null || languageCode.isBlank()) {
      return false;
    }
    if (
      languageCode.contains("/") ||
      languageCode.contains("\\") ||
      languageCode.contains("..")
    ) {
      return false;
    }
    return true;
  }

  private static DefaultsData loadDefaultsFromPlugin(JavaPlugin plugin) {
    Map<String, Object> raw = loadRawDefaults(plugin);
    if (raw == null) {
      return new DefaultsData(
        Map.of(),
        Map.of(),
        Map.of(),
        Map.of(),
        Set.of()
      );
    }

    Map<String, String> strings = new HashMap<>();
    Object stringsObj = raw.get("strings");
    if (stringsObj instanceof Map<?, ?> map) {
      for (var entry : map.entrySet()) {
        if (entry.getValue() instanceof String s) {
          strings.put((String) entry.getKey(), s);
        }
      }
    }

    Map<String, String> languageNames = new HashMap<>();
    Map<String, List<String>> credits = new HashMap<>();
    Map<String, String> aliasToCode = new HashMap<>();
    Set<String> canonicalCodes = new HashSet<>();

    Object langObj = raw.get("languages");
    if (langObj instanceof Map<?, ?> groups) {
      for (Object groupObj : groups.values()) {
        if (groupObj instanceof Map<?, ?> group) {
          for (var entry : group.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> langInfo) {
              String code = (String) entry.getKey();
              canonicalCodes.add(code);

              Object name = langInfo.get("name");
              if (name instanceof String s) {
                languageNames.put(code, s);
              }

              Object creditsObj = langInfo.get("credits");
              if (creditsObj instanceof List<?> list) {
                List<String> creditList = new ArrayList<>();
                for (Object item : list) {
                  if (item instanceof String s && !s.isBlank()) {
                    creditList.add(s);
                  }
                }
                if (!creditList.isEmpty()) {
                  credits.put(code, creditList);
                }
              }

              // Generate aliases at runtime from the canonical code
              // e.g. en_US -> en_US, en_us, en-US, en-us
              aliasToCode.put(code.toLowerCase(Locale.ROOT), code);
              aliasToCode.put(code.replace('_', '-').toLowerCase(Locale.ROOT), code);
              aliasToCode.put(code, code);

              Object defaultObj = langInfo.get("default");
              if (Boolean.TRUE.equals(defaultObj)) {
                // Extract the base language form (e.g. en_US -> en)
                String base = code.contains("_")
                  ? code.substring(0, code.indexOf('_'))
                  : code;
                aliasToCode.put(base.toLowerCase(Locale.ROOT), code);
              }
            }
          }
        }
      }
    }

    return new DefaultsData(
      strings,
      languageNames,
      credits,
      aliasToCode,
      canonicalCodes
    );
  }

  private static Map<String, Object> loadRawDefaults(JavaPlugin plugin) {
    File dataFile = new File(
      plugin.getDataFolder(),
      "lang" + File.separator + "defaults.json"
    );
    if (dataFile.isFile()) {
      try (InputStream in = new FileInputStream(dataFile)) {
        return parseRawJson(in);
      } catch (IOException | JsonSyntaxException e) {
        plugin
          .getLogger()
          .log(Level.WARNING, "Failed to parse custom defaults.json, using bundled", e);
      }
    }

    try (InputStream in = plugin.getResource("lang/defaults.json")) {
      if (in != null) {
        return parseRawJson(in);
      }
    } catch (IOException | JsonSyntaxException e) {
      plugin
        .getLogger()
        .log(Level.WARNING, "Failed to parse bundled defaults.json", e);
    }

    return null;
  }

  private static Map<String, Object> parseRawJson(InputStream in)
    throws IOException {
    try (
      InputStreamReader reader = new InputStreamReader(
        in,
        StandardCharsets.UTF_8
      )
    ) {
      return new Gson().fromJson(
        reader,
        new TypeToken<Map<String, Object>>() {}.getType()
      );
    }
  }

  private static Map<String, String> parseJson(InputStream in)
    throws IOException {
    try (
      InputStreamReader reader = new InputStreamReader(
        in,
        StandardCharsets.UTF_8
      )
    ) {
      Map<String, Object> raw = new Gson().fromJson(
        reader,
        new TypeToken<Map<String, Object>>() {}.getType()
      );
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
}
