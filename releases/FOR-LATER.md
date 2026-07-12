# For Later

This file contains updates that we need to document in a later release when that comes.

## Changes

- `/dd locale credits` and `/dd locale credit <code>` for listing translation credits.
- Completed the locale command strings across bundled translations instead of leaving English fallbacks in non-English language files.
- Migrated the build and release pipeline from Maven to Gradle, including the Bukkit/Velocity module builds, the Gradle wrapper, and the GitHub Actions `publish` workflow.
- Added a GitHub Actions CI workflow that builds the project on pushes and pull requests.
- Clarified translation guidelines for preserving language-specific characters.
- Hardened FastStats token normalization to use locale-independent lowercasing.
- Ensured SQL schema migrations add newer preference and proxy columns on existing SQLite databases as well as other SQL dialects.
- Ensured YAML-to-SQL migration success is reported only after the migration marker is persisted.
- Added the missing unknown-locale credit message to regional language files and corrected locale translation typos.
- Preserved newer queued YAML player-preference saves when an older asynchronous save batch completes.
- Kept redstone-triggered double-door synchronization active for ordinary doors when recursive opening is disabled.
- Preserved custom openable handling in API-triggered linked openings.
- Bypassed WorldGuard region allow/deny filtering when region lookup is unavailable instead of treating it as an empty region set.
