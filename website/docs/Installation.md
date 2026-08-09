---
title: Installation
id: Installation
slug: /Installation
---

## Requirements

- Paper or Spigot 1.21+
- Java 25+ runtime on the server

## Download

Get the latest release jar from:
https://github.com/SzaBee13/double-doors-server/releases

## Install Steps

1. Stop your server.
2. Place the plugin jar into the plugins folder.
3. Start the server once to generate config files.
4. Verify plugin startup in the console log.

Expected startup info includes lines similar to:
- DoubleDoors enabled.
- Compatibility notices for LuckPerms, GriefPrevention, or Geyser/Floodgate if present.

## Generated Files

- plugins/DoubleDoors/config.yml
- plugins/DoubleDoors/players.yml

## Upgrade Steps

1. Stop the server.
2. Replace the old jar with the new one.
3. Start server and check for config key additions.
4. Merge any missing keys from the default config if needed.

## Verify Installation

- Use command: /doubledoors reload
- Right-click one side of a mirrored double door.
- Confirm the partner door mirrors the final open/close state.
