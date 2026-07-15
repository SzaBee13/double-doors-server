# DoubleDoors v1.4.7 Release Notes

Release date: 

## Highlights

- Translation credit commands for locale contributors.
- Improved SQL migration reliability and async persistence safety.
- Redstone door sync now works correctly when recursive opening is disabled.

## Added

- `/dd locale credits` and `/dd locale credit <code>` for listing translation credits.
- Expanded FastStats metcis with anonymous server configuration, feature-use, integration, capacity, and uptime metrics, plus automatic error tracking.

## Changed

- Migrated the build and release pipeline from Maven to Gradle, including the Bukkit/Velocity module builds, the Gradle wrapper, and the GitHub Actions `publish` workflow.
- Added a GitHub Actions CI workflow that builds the project on pushes and pull requests.
- Ensured SQL schema migrations add newer preference and proxy columns on existing SQLite databases as well as other SQL dialects.
- Bypassed WorldGuard region allow/deny filtering when region lookup is unavailable instead of treating it as an empty region set.
- Preserved custom openable handling in API-triggered linked openings.
- Updated help command output to look cleaner.

## Fixed

- Ensured YAML-to-SQL migration success is reported only after the migration marker is persisted.
- Preserved newer queued YAML player-preference saves when an older asynchronous save batch completes.
- Kept redstone-triggered double-door synchronization active for ordinary doors when recursive opening is disabled.
- Prevented redstone signal loss on one source from closing doors and fence gates that are still independently powered by another signal.

## Breaking Changes

None.

## Upgrade Guide

1. Back up your server folder and existing plugin data.
2. Replace the old jar(s) with the new release artifact(s).
3. Start the server once to generate or update configuration files.
4. Review and adjust new config keys.
5. Validate expected behavior in-game.

## Artifacts

- Bukkit/Spigot/Paper/Purpur: doubledoors-bukkit-1.4.7.jar
- Velocity: doubledoors-velocity-1.4.7.jar
