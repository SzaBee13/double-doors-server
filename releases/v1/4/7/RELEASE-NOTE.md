# DoubleDoors v1.4.7 Release Notes

Release date: 

## Highlights

- Translation credit commands for locale contributors.
- Improved SQL migration reliability and async persistence safety.
- Redstone door sync now works correctly when recursive opening is disabled.

## Added

- `/dd locale credits` and `/dd locale credit <code>` for listing translation credits.
- Expanded FastStats metrics with anonymous server configuration, configured data-storage-type reporting, feature-use, integration, capacity, and uptime metrics, plus automatic error tracking.
- Custom translation file support — set `language: custom` in config.yml, then edit `plugins/DoubleDoors/custom_lang.json` (auto-generated from en_US on first use). Re-reads on `/dd reload`.

## Changed

- Separated Paper/Purpur/Folia artifacts from Bukkit/Spigot artifacts.
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

## Extended Release Notes

<details>
<summary>Developer Notes</summary>
<ul>
  <li>Removed unused dependencies.</li>
  <li>Moved from Maven to Gradle Kotlin DSL.</li>
  <li>Refactored Release Notes structure</li>
  <li>Moved from Dependabot to Renovate</li>
  <li>Changed the language structure (see [defaults.json](https://github.com/SzaBee13
    /double-doors-server/tree/v1.4.7/core/src/main/resources/lang/defaults.json))</li>
  <li>Added a compact Paper/Purpur/Folia artifact that downloads SQL libraries through Paper at startup; the Bukkit/Spigot artifact remains self-contained.</li>
  <li>Refactored translation loading to support a custom file path independent of the locale system.</li>
</ul>
</details>

## Upgrade Guide

1. Back up your server folder and existing plugin data.
2. Ensure your server runs Java 25 or later. (Already a requirement on 26.1+ for clients and servers)
3. Replace the old jar(s) with the new release artifact(s).
4. Start the server once to generate or update configuration files.
5. Review and adjust new config keys.
6. Validate expected behavior in-game.

## Artifacts

- Bukkit/Spigot: doubledoors-bukkit-1.4.7.jar
- Paper/Purpur/Folia: doubledoors-paper-1.4.7.jar
- Velocity: doubledoors-velocity-1.4.7.jar
