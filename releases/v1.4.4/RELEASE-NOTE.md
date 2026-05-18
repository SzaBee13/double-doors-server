# DoubleDoors v1.4.4 Release Notes

Release date: 2026-05-18

## Highlights

- Relaxed the default GriefPrevention trust level required to open linked double doors from Build to Access.
- Added a configuration option to retain the stricter behavior.

## Added

- Config option `griefprevention.requireBuildForLinkedDoors` (default `false`) for server owners who prefer requiring Build permission for linked door interactions in GriefPrevention claims.

## Fixed

- Addressed an issue where players with AccessTrust or ContainerTrust in a GriefPrevention claim were incorrectly blocked from opening double doors.

## Breaking Changes

- None.

## Upgrade Guide

1. Back up your server folder and existing plugin data.
2. Replace the old jar(s) with the new release artifact(s).
3. Start the server once to generate or update configuration files.
4. Review and adjust new config keys.
5. Validate expected behavior in-game.

## Artifacts

- Bukkit/Spigot/Paper/Purpur: doubledoors-bukkit-1.4.4.jar
- Optional proxy companion: doubledoors-proxy-1.4.4.jar

## Notes

- By default, Access trust is now sufficient to open linked doors within a GriefPrevention claim. Set `requireBuildForLinkedDoors` to `true` in config.yml to revert to the old requirement.