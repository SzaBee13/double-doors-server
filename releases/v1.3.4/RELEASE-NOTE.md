# DoubleDoors v1.3.4 Release Notes

Release date: 2026-04-26

## Highlights

- Fixed a plugin updater initialization crash that prevented update checks from running on some server environments.

## Added

- Added a config note to config.yml explaining that the built-in updateChecker is disabled automatically when a separate PluginUpdater plugin is present.

## Fixed

- Resolved an issue where the PluginUpdater integration could fail during startup with `UnsupportedOperationException` while registering the Modrinth platform.
- Fixed fence gate linked-opening alignment to respect the gate's placed axis (north/south vs east/west) instead of player look direction, preventing occasional wrong-direction gate rotation.

## Breaking Changes

- None.

## Upgrade Guide

1. Back up your server folder and existing plugin data.
2. Replace the old jar(s) with the new release artifact(s).
3. Start the server once to generate or update configuration files.
4. Review and adjust new config keys if needed.
5. Validate expected behavior in-game.

## Artifacts

- Bukkit/Paper: doubledoors-bukkit-1.3.4.jar
- Optional proxy companion: doubledoors-proxy-1.3.4.jar

## Notes

- This release includes updater compatibility improvements and a fence gate linked-opening orientation fix.
