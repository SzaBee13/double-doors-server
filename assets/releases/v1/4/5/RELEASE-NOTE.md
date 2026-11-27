# DoubleDoors v1.4.5 Release Notes

Release date: 2026-06-17

## Highlights

- Added Minecraft 26.2 (Java Edition 26.2) compatibility.
- Localization sync from Crowdin with community-contributed translations across all supported locales.
- Dependency updates: FastStats 0.26.1, HikariCP 7.1.0, SQLite 3.53.2.0, Surefire 3.5.6.

## Added

- New Maven profile `mc-26.2.x` — build with `-P mc-26.2.x` to target Spigot 26.2 API (`26.2-R0.1-SNAPSHOT`). Now the default build profile.
- `src/main/java-v26_2_x` version bridge implementation for Minecraft 26.2.x.

## Changed

- Default build target updated from 26.1 to 26.2.

## Fixed

- None.

## Breaking Changes

- None. Minecraft 26.2 has no significant API breakages for API-based plugins. The existing 26.1 builds remain available via `-P mc-26.1.x`.

## Upgrade Guide

1. Back up your server folder and existing plugin data.
2. Ensure your server is running Minecraft 26.2 (Paper/Spigot).
3. Replace the old jar(s) with the new release artifact(s).
4. Start the server once to generate or update configuration files.
5. Review and adjust new config keys.
6. Validate expected behavior in-game.

## Artifacts

- Bukkit/Spigot/Paper/Purpur: doubledoors-bukkit-1.4.5.jar
- Optional proxy companion: doubledoors-proxy-1.4.5.jar

## Notes

- The 26.1 build profile is retained for servers that have not yet upgraded to 26.2. Use `mvn package -P mc-26.1.x` to produce a 26.1-compatible build.
