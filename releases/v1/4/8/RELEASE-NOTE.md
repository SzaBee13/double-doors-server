# DoubleDoors v1.4.8 Release Notes

Release date: 2026-08-23

## Highlights

- Velocity plugin now auto-downloads SQL dependencies on first start with SHA-256 verification, reducing jar size and simplifying updates.
- Protection integrations (GriefPrevention, WorldGuard) now fail closed instead of fail open, improving security.
- Telemetry cleanup: removed stale metrics, added per-player locale and update-checker tracking.

## Added

- Automatic SQL library downloader for Velocity — sqlite-jdbc, mysql-connector-j, and HikariCP are fetched from Maven Central, checksum-verified, and cached under `plugins/doubledoors-velocity/libs/`.
- Per-player locale metric reporting to FastStats.

## Changed

- Changed `ProtectionCompat` from fail-open to fail-closed when GriefPrevention or WorldGuard checks cannot be resolved. Absent integrations are still bypassed.
- Changed Velocity default SQLite JDBC URL from absolute to relative (`jdbc:sqlite:plugins/DoubleDoors/doubledoors.db`). Absolute paths are still recommended for proxy↔Bukkit sharing.
- Removed bundled SQL libraries from the Velocity jar; they are now downloaded at runtime.
- Updated FastStats public URL to `https://faststats.dev/project/double-doors-server`.

## Fixed

- Fixed FastStats metric collection invoking Bukkit APIs from asynchronous threads; `per_player_locales` and `update_checker` are now snapshotted on the main thread, preventing corrupted or failed telemetry submissions.
- Fixed GriefPrevention and WorldGuard integration checks logging at warning level instead of fine when checks fail.
- Fixed unresolved GriefPrevention and WorldGuard reflection checks so configured protection rules deny safely.
- Corrected configuration examples, safety guidance, and French and Brazilian Portuguese command translations.

## Breaking Changes

- Protection integrations now fail closed: if GriefPrevention or WorldGuard is installed but their APIs cannot be resolved, linked door interactions inside claims/regions will be blocked instead of allowed. If you rely on these integrations, verify they are up to date and functioning.
- Velocity operators may see a brief delay on first startup while SQL libraries are downloaded. Subsequent starts use the cached JARs.

## Extended Release Notes

<details>
<summary>Developer Notes</summary>
<ul>
  <li>Migrated to Docusaurus documentation site with versioned 1.4 and 1.3 wiki snapshots.</li>
  <li>Fixed Renovate schedule.</li>
  <li>Telemetry cleanup — removed: <code>geyser_detected</code>, <code>worldguard_detected</code>, <code>griefprevention_detected</code>, <code>plugin_uptime_minutes</code>, <code>server_max_players</code>. Added: <code>update_checker</code>, <code>per_player_locales</code>. Changed: <code>recursive_opening</code> from boolean to number.</li>
  <li>Changed Velocity SQL dependencies from <code>implementation</code> to <code>compileOnly</code> and added runtime downloader with classpath injection.</li>
  <li>Added SHA-256 checksums for SQL dependency versions in <code>gradle.properties</code>.</li>
  <li>FastStats collectors no longer touch live Bukkit state: <code>FastStatsManager</code> now maintains main-thread-computed snapshots (refreshed every 60 s) that the async collectors read.</li>
  <li>Added unit test coverage for <code>ProtectionCompat</code> (including the fail-closed deny reasons and claim-manager fail-open behavior) and <code>FastStatsManager</code>.</li>
  <li>Cleaned up unused import in <code>ProtectionCompat</code>.</li>
</ul>
</details>

## Upgrade Guide

1. Back up your server folder and existing plugin data.
2. Replace the old jar(s) with the new release artifact(s).
3. Start the server once to generate or update configuration files.
4. If using GriefPrevention or WorldGuard, ensure the plugins are installed and up to date. Their APIs must be resolvable or linked door interactions inside claims/regions will be blocked.
5. For Velocity: the first startup will download SQL libraries to `plugins/doubledoors-velocity/libs/`. Ensure the server has internet access on first start.
6. Review and adjust new config keys.
7. Validate expected behavior in-game.

## Artifacts

- Bukkit/Spigot: doubledoors-bukkit-1.4.8.jar
- Paper/Purpur/Folia: doubledoors-paper-1.4.8.jar
- Velocity: doubledoors-velocity-1.4.8.jar
