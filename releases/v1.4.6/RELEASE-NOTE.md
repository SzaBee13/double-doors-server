# DoubleDoors v1.4.6 Release Notes

Release date: 2026-07-03

## Highlights

- Per-player locale overrides via `/dd locale <code>` when enabled in config.
- Locale preference is persisted and syncs across servers through SQL.

## Added

- `perPlayerLocaleEnabled` config toggle to allow or block the locale command.
- `/dd locale` command support for setting, viewing, and clearing a player's locale.

## Changed

- Renamed `doubledoors-proxy` to `doubledoors-velocity`.
- Split shared DoubleDoors code into a new `core` module for future loader support.
- Moved the public API and translation contract into `core` so Bukkit stays loader-focused.
- Optimized the Crowdin GitHub workflow to cancel superseded runs and sync locale files directly in `core/src/main/resources/lang`.
- Updated Crowdin config/workflow to use direct translation paths in `core/src/main/resources/lang/%locale_with_underscore%.json` and keep `es_409`/`es_419` aliasing compatible.

## Fixed

- Hardened proxy JDBC driver detection for uppercase URL prefixes.
- Reused the same schema connection during proxy heartbeat table initialization.
- Added regression coverage for proxy SQL client URL handling.
- Fixed SQL upsert statements in shared storage to support both SQLite (`ON CONFLICT`) and MySQL (`ON DUPLICATE KEY`), restoring SQL mode with default SQLite configuration.
- Fixed Bukkit version bridge bootstrap class lookup so version-specific bridge loading works again.
- Enforced the `doubledoors.locale` permission for `/dd locale`.
- Serialized claim-settings persistence writes and switched SQL claim updates to current-state writes to avoid async persistence races.
- Ensured Velocity always closes the SQL client on shutdown, even if heartbeat initialization failed.
- Removed pre-delay block-data reads from MONITOR listener paths by deferring knock-state reads and using material-only type gating in event handlers.

## Upgrade Guide

1. Back up your server folder and existing plugin data.
2. Replace the old jar(s) with the new release artifact(s).
3. Start the server once to generate or update configuration files.
4. Review and adjust new config keys.
5. Validate expected behavior in-game.

## Artifacts

- Bukkit/Spigot/Paper/Purpur: doubledoors-bukkit-1.4.6.jar
- Velocity: doubledoors-velocity-1.4.6.jar

## Notes

- If you update the velocity proxy, you need to move the config directory in the plugins folder from `plugins/doubledoors-proxy/config.properties` to `plugins/doubledoors-velocity/config.properties`.