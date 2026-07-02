# DoubleDoors v1.4.6 Release Notes

Release date: 2026-06-29

## Highlights

- Per-player locale overrides via `/dd locale <code>` when enabled in config.
- Locale preference is persisted and syncs across servers through SQL.

## Added

- `perPlayerLocaleEnabled` config toggle to allow or block the locale command.
- `/dd locale` command support for setting, viewing, and clearing a player's locale.

## Changed

- Version bumped to `1.4.6` in the Maven POMs.

## Fixed

- Hardened proxy JDBC driver detection for uppercase URL prefixes.
- Reused the same schema connection during proxy heartbeat table initialization.
- Added regression coverage for proxy SQL client URL handling.
