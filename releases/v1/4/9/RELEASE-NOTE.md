# DoubleDoors v1.4.9 Release Notes

Release date: 2026-09-

## Highlights

- Full support for Minecraft 26.3, including Poplar doors, trapdoors, and fence gates.
- Resolved WorldGuard compatibility issues on newer releases by adding `RegionQuery` fallback for build permission checks.
- Integrated search functionality across the official documentation website.

## Added

- Minecraft 26.3 support (`v26_3_x` version bridge implementation).
- Poplar wood type support for doors, trapdoors, and fence gates.
- Algolia DocSearch integration on the documentation website.
- Environment file support (`.env.example`) for website development tooling.

## Changed

- Updated GitHub publish workflow to dynamically read supported game versions from `assets/actions/publish/game_versions.txt`.
- Refactored `ProtectionCompat` unit tests with a dedicated `WorldGuardPlugin` stub.
- Clarified supported platform and plugin version ranges in the Bukkit compatibility documentation.

## Fixed

- Fixed WorldGuard build-permission compatibility when legacy `WorldGuardPlugin.canBuild()` method is absent in newer WorldGuard versions.

## Breaking Changes

None.

## Extended Release Notes

<details>
<summary>Developer Notes</summary>
<ul>
  <li>Added <code>src/main/java-v26_3_x</code> version bridge implementation and unit tests.</li>
  <li>Updated Paper dev bundle dependency to 26.3.</li>
  <li>Added <code>RegionQuery.testState(..., Flags.BUILD)</code> fallback logic in <code>ProtectionCompat</code> for WorldGuard.</li>
  <li>Added comprehensive unit tests for <code>ProtectionCompat</code>.</li>
  <li>Added game version manifest file at <code>assets/actions/publish/game_versions.txt</code>.</li>
  <li>Updated CodeRabbit and Renovate configuration.</li>
</ul>
</details>

## Upgrade Guide

1. Back up your server folder and existing plugin data.
2. Replace the old jar(s) with the new release artifact(s).
3. Start the server once to generate or update configuration files.
4. Review and adjust new config keys.
5. Validate expected behavior in-game.

## Artifacts

- Bukkit/Spigot: doubledoors-bukkit-1.4.9.jar
- Paper/Purpur/Folia: doubledoors-paper-1.4.9.jar
- Velocity: doubledoors-velocity-1.4.9.jar
