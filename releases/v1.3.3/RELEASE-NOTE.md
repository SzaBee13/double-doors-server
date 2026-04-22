# DoubleDoors v1.3.3 Pre Release Notes

Release date: 2026-04-22

## Highlights

- Upgraded the project to Java 25 and released updated Bukkit and proxy artifacts.
- Added WorldGuard-aware protection support, including region filtering and the custom `double-doors-allow` flag.
- Added configurable partner sounds, particles, debug diagnostics, and a preview command for troubleshooting linked doors.

## Added

- Added soft-depend support for WorldGuard.
- Added WorldGuard region blacklist/whitelist support.
- Added optional WorldGuard custom flag support via `double-doors-allow`.
- Added configurable linked-partner sound playback with per-door-type overrides.
- Added optional subtle particles on linked partner blocks.
- Added `/doubledoors debug` for partner lookup diagnostics.
- Added `/doubledoors preview` for showing linked-door location and facing.
- Added built-in PluginUpdater API support for automatic Modrinth update checks.
- Added a public `DoubleDoorsAPI` for other plugins.
- Added custom openable registration support for external plugins.

## Changed

- Upgraded the build target to Java LTS 25.
- Added location blacklist/whitelist filtering for specific coordinates.
- Added lookup caching for repeated partner searches.
- Improved animation sync timing with configurable extra delay ticks.
- Updated command usage, permissions, and configuration defaults for the new features.

## Fixed

- Improved linked-door diagnostics so operators can see why a partner block was skipped.
- Tightened reflective protection checks to remain fail-open when optional plugins are unavailable.
- Fixed the proxy's handling of database connection failures to prevent server instability.

## Breaking Changes

- Java 25 runtime is now required for all `1.3.3` artifacts (`maven.compiler.release=25`). Servers still running older JVMs must be upgraded before deployment.

## Upgrade Guide

1. Back up your server folder and existing plugin data.
2. Upgrade your server runtime to Java 25 before deploying this release. This is mandatory and breaking for older JVM versions.
3. Replace the old jars with the `1.3.3` release artifacts.
4. Make sure your build environment uses Java 25 if you are compiling from source.
5. Review the new config keys in `config.yml`, especially the WorldGuard, sound, particle, and filter options.
6. Verify plugin metadata remains in sync with this runtime target (`bukkit/src/main/resources/plugin.yml` version and compatibility fields).
7. Send a pre-deployment notification to operators that Java 25 is required before rollout.
8. Test `/doubledoors debug` and `/doubledoors preview` on a live server to verify the new behavior.

## Artifacts

- Bukkit/Paper: `doubledoors-bukkit-1.3.3.jar`
- Optional proxy companion: `doubledoors-proxy-1.3.3.jar`

## Notes

- WorldGuard integration is optional and fail-open if the plugin is not installed or reflective checks cannot be resolved.
- Linked-partner sound and particle effects are configurable and can be disabled entirely in `config.yml`.
- The preview command is intended for operators and troubleshooting, not normal gameplay.
