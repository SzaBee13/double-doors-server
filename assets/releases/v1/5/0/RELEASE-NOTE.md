# DoubleDoors v1.5.0 Release Notes

Release date: 2026-11-27

## Added

- Added automatic compatibility with GriefPrevention3D. Its 3D claims are checked at the door's actual height without additional DoubleDoors configuration; servers using only regular 2D claims retain the original GriefPrevention behavior.

## Fixed

- Log a warning when saving a claim setting to SQL fails, instead of silently dropping the failure.
- Restored missing diacritical marks in German, Spanish, French, and Portuguese locale strings.

## Breaking Changes

None

## Extended Release Notes

<details>
<summary>Developer Notes</summary>
<ul>
  <li>Fixed the redstone listener test to provide a mocked Bukkit scheduler and verify the villager-event delay.</li>
  <li>Moved the Bukkit, Paper, and Velocity loader modules under `loaders/` while preserving their Gradle project names (`:bukkit`, `:paper`, and `:velocity`).</li>
  <li>Separated the loader-neutral API into `core`, added a typed Bukkit API facade, and moved Paper-specific command initialization into the Paper loader.</li>
  <li>Registered the plugin's permission nodes and defaults in the Paper loader, since Paper's plugin descriptor does not load permissions from `plugin.yml`.</li>
</ul>
</details>

<details>
<summary>Dependency Updates</summary>
<div class="table-wrapper"><table><thead><tr><th align="left">Package</th><th align="right">Old Version</th><th align="right">New Version</th></tr></thead><tbody><tr><td align="left">io.papermc.paperweight.userdev</td><td align="right">v2.0.0-beta.21</td><td align="right">v2.0.0-beta.24</td></tr><tr><td align="left">org.junit:junit-bom</td><td align="right">6.1.1</td><td align="right">6.1.3</td></tr><tr><td align="left">com.gradleup.shadow</td><td align="right">9.5.1</td><td align="right">9.6.1</td></tr><tr><td align="left">dev.faststats.metrics</td><td align="right">0.27.2</td><td align="right">0.32.2</td></tr><tr><td align="left">com.google.code.gson:gson</td><td align="right">2.14.0</td><td align="right">2.15.0</td></tr><tr><td align="left">com.mysql:mysql-connector-j</td><td align="right">9.7.0</td><td align="right">26.10.0</td></tr><tr><td align="left">actions/setup-java</td><td align="right">v5</td><td align="right">v6</td></tr><tr><td align="left">crowdin/github-action</td><td align="right">v2</td><td align="right">v3</td></tr><tr><td align="left">org.xerial:sqlite-jdbc</td><td align="right">3.53.2.0</td><td align="right">3.54.1.0</td></tr><tr><td align="left">org.mockito:mockito-junit-jupiter</td><td align="right">5.23.0</td><td align="right">5.24.0</td></tr><tr><td align="left">org.mockito:mockito-core</td><td align="right">5.23.0</td><td align="right">5.24.0</td></tr></tbody></table></div>
</details>

## Upgrade Guide

1. Back up your server folder and existing plugin data.
2. Replace the old jar(s) with the new release artifact(s).
3. Start the server once to generate or update configuration files.
4. Review and adjust new config keys.
5. Validate expected behavior in-game.

## Artifacts

- Bukkit/Spigot: doubledoors-bukkit-1.5.0.jar
- Paper/Purpur/Folia: doubledoors-paper-1.5.0.jar
- Velocity: doubledoors-velocity-1.5.0.jar
