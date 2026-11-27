# DoubleDoors v1.3.4 Release Notes

Release date: 2026-04-26

## Highlights

- Fixed a PluginUpdater API integration issue that could break DoubleDoors update-check initialization during startup.
- Clarified updater ownership when a standalone `PluginUpdater` plugin is installed to prevent duplicate notifications.

## Added

- Added startup logging that clearly states which updater path is active:
  - built-in DoubleDoors updater checks enabled,
  - built-in checks disabled by config,
  - or checks delegated to standalone `PluginUpdater`.
- Added a config note in `config.yml` explaining that DoubleDoors built-in update checks are disabled automatically when a standalone `PluginUpdater` plugin is present.

## Fixed

- Resolved an issue where the shaded PluginUpdater API integration could fail during startup with `UnsupportedOperationException` while registering the Modrinth platform.
- Fixed fence gate linked-opening alignment to respect the gate's placed axis (north/south vs east/west) instead of player look direction, preventing occasional wrong-direction gate rotation.

## Breaking Changes

- None.

## Upgrade Guide

1. Back up your server folder and existing plugin data.
2. Replace the old jar(s) with the new release artifact(s).
3. Start the server once to generate or update configuration files.
4. Review `updateChecker.*` settings in `config.yml` and confirm they match your intended notification behavior.
5. Check startup logs for one of these messages:
   - built-in updater checks enabled,
   - built-in checks disabled by config,
   - delegation to standalone `PluginUpdater`.
6. If `PluginUpdater` is installed separately, verify that plugin is configured to include DoubleDoors in its update checks.
7. Validate expected behavior in-game.

## Artifacts

- Bukkit/Paper: doubledoors-bukkit-1.3.4.jar
- Optional proxy companion: doubledoors-proxy-1.3.4.jar

## Notes

- If you do not receive update notifications after upgrading, first verify whether DoubleDoors is using built-in checks or delegating to standalone `PluginUpdater`, then verify the active updater configuration.
- This release includes updater compatibility improvements and a fence gate linked-opening orientation fix.
