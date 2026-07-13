# AGENTS.md — AI Agent Guide for DoubleDoors

This file tells AI coding agents how to work effectively in this repository.

## Repository at a glance

| Item | Value |
| --- | --- |
| Language | Java 25 |
| Framework | Paper/Spigot 1.21.x + 26.2 plugin API |
| Build | Gradle (`./gradlew build`) |
| Output | `bukkit/build/libs/doubledoors-bukkit-<version>.jar`, `velocity/build/libs/doubledoors-velocity-<version>.jar` (shaded) |

## Source Control (Git)

- Every change should be committed with a clear, descriptive commit message.
- Use branches for feature development and pull requests for code review.

## Key source files

| File | Role |
| --- | --- |
| [`DoubleDoors.java`](bukkit/src/main/java-common/me/szabee/doubledoors/bukkit/DoubleDoors.java) | Plugin lifecycle, command handling, tab completion |
| [`PluginConfig.java`](bukkit/src/main/java-common/me/szabee/doubledoors/bukkit/config/PluginConfig.java) | Reads/writes `config.yml` (server-wide settings) |
| [`PlayerPreferences.java`](bukkit/src/main/java-common/me/szabee/doubledoors/bukkit/config/PlayerPreferences.java) | Reads/writes `players.yml` (per-player settings, persistent) |
| [`DoorInteractListener.java`](bukkit/src/main/java-common/me/szabee/doubledoors/bukkit/listeners/DoorInteractListener.java) | Player right-click linking logic |
| [`RedstoneListener.java`](bukkit/src/main/java-common/me/szabee/doubledoors/bukkit/listeners/RedstoneListener.java) | Redstone power changes + villager AI door events |
| [`DoorUtil.java`](bukkit/src/main/java-common/me/szabee/doubledoors/bukkit/util/DoorUtil.java) | Block-search utilities (mirrored partner, BFS connected set) |
| [`ProtectionCompat.java`](bukkit/src/main/java-common/me/szabee/doubledoors/bukkit/util/ProtectionCompat.java) | Reflective GriefPrevention integration |
| [`TranslationManager.java`](bukkit/src/main/java-common/me/szabee/doubledoors/bukkit/i18n/TranslationManager.java) | Locale-aware translation lookups (player vs. console) |
| [`TranslationCatalog.java`](bukkit/src/main/java-common/me/szabee/doubledoors/bukkit/i18n/TranslationCatalog.java) | Language file loading, available-languages list |

## Coding conventions

- Java 26 features are expected (records, switch expressions, pattern-matching `instanceof`).
- All public methods require Javadoc.
- `final` on classes/fields unless mutability is necessary.
- No wildcard imports.
- Use **2 spaces** for indentation throughout the repository.
- Block data must only be read **after** a scheduled delay — never inline in an event handler (see delay constants in `RedstoneListener`).

## Versioning

### Version bumping checklist

When releasing a new version:

1. Update `version` in `gradle.properties`.
2. Update `version:` in `bukkit/src/main/resources/plugin.yml`.
3. Create `releases/v<version>/RELEASE-NOTE.md` using `releases/RELEASE-NOTE-EXAMPLE.md` as a template. DO NOT add a release date to the top of the file.
4. If `javaRelease` changed, call out the new required Java runtime in release notes/upgrade guide and notify operators before deployment.

### Release notes

You should update `releases/v<version>/RELEASE-NOTE.md` with any changes from `FOR-LATER.md` before releasing.
If the release date is in the past, instead of updating the file with the release notes, put the changes in `FOR-LATER.md` and update the file with the release notes when the release date is updated.

## Dangerous areas

- `ProtectionCompat` uses unchecked reflection — changes here need careful null/exception handling.
- `PlayerPreferences.save()` writes to disk; only call it via `saveAsync()` from the main thread.
- The Geyser/Floodgate duplicate-interaction debounce in `DoorInteractListener` uses nanosecond timestamps — do not remove it without understanding Bedrock client double-fire behavior.

## Localization

- Only `en_US` is updated by default; other locales inherit from it until explicitly localized, but add the keys in the other language files and leave it empty.
- When adding/modifying message keys, update `core/src/main/resources/lang/defaults.json` and `core/src/main/resources/lang/en_US.json` first.  
- If localization for other languages is requested, propagate changes to all regional variants (en_GB, fr_FR, de_DE, etc.) to maintain consistency.
- See `TRANSLATING.md` and skill translation for details on contributing translations via Crowdin.

## Documentation

EVERY CHANGE **MUST** BE DOCUMENTED IN `releases/v<version>/RELEASE-NOTE.md`.
If necessary, update the GitHub Wiki at `wiki/`.
