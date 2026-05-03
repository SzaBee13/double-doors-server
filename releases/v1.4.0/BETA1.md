# DoubleDoors v1.4.0 BETA.1 Release Notes

Release date: 2026-05-02

## WARNING

This is a BETA release intended for testing purposes only. It may contain bugs or incomplete features. Do not use this version on production servers.

## Highlights

- Added new commands to toggle auto-close and knock sounds.
- Improved server compatibility: DoubleDoors is now fully compatible with Folia.
- Added missing translations for all supported languages.

## Added

- Added personal toggle commands for `autoclose` and `knock` sounds (`/doubledoors toggle [autoclose|knock]`).
- Added personal `knock-volume` command (`/doubledoors knock-volume <0-1>`).
- Added missing translations across all language files.
- Added support for corner doors in the plugin's internal door detection logic.

## Changed

- Scheduled tasks now natively support Folia through region and global schedulers.

## Fixed

- Fixed missing localization keys across various languages.

## Upgrade Guide

1. Back up your server folder and existing plugin data.
2. Replace the old jar(s) with the new release artifact(s).
3. Start the server once to generate or update configuration files.
4. Review and adjust new config keys.
5. Validate expected behavior in-game.

## Artifacts

- Bukkit/Spigot/Paper/Purpur: doubledoors-bukkit-1.4.0-beta.1.jar
- Optional proxy companion: doubledoors-proxy-1.4.0-beta.1.jar

## Notes

- Folia operators should notice improved plugin stability due to proper task scheduling.
