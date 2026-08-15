---
title: Commands and Permissions
id: Commands-and-Permissions
slug: /Commands-and-Permissions
---

## Commands

Short alias: `dd`

### /doubledoors reload

- Permission: doubledoors.reload
- Sender: console or player
- Effect: Reloads config.yml and players.yml cache.

### /doubledoors toggle

- Permission: doubledoors.toggle
- Sender: player only
- Effect: Toggles the player global preference for linked behavior.

### /doubledoors toggle doors

- Permission: doubledoors.toggle
- Sender: player only
- Effect: Toggles only door linking for that player.

### /doubledoors toggle gates

- Permission: doubledoors.toggle
- Sender: player only
- Effect: Toggles only fence-gate linking for that player.

### /doubledoors toggle trapdoors

- Permission: doubledoors.toggle
- Sender: player only
- Effect: Toggles only trapdoor linking for that player.

### /doubledoors toggle autoclose

- Permission: doubledoors.toggle.autoclose
- Sender: player only
- Effect: Toggles only auto-close behavior for that player.

### /doubledoors toggle knock

- Permission: doubledoors.toggle.knock
- Sender: player only
- Effect: Toggles only knock-sound behavior for that player.

### /doubledoors knock-volume

- Permission: doubledoors.knock.volume
- Sender: player only
- Effect: Sets the player's personal knock sound volume (0.0 to 1.0).
- Usage: `/doubledoors knock-volume &lt;0-1&gt;`

### /doubledoors server-toggle

- Permission: doubledoors.server-toggle
- Sender: console or player
- Effect: Flips serverWideEnabled and persists to config.

### /doubledoors locale

- Permission: `doubledoors.locale`
- Sender: player only
- Effect: Shows the player’s current locale, the server default, and the available language list.
- Requires `perPlayerLocaleEnabled: true` in `config.yml`.

Supported subcommands:

- `/doubledoors locale &lt;code&gt;` sets the player locale override, for example `de_DE` or `fr_FR`.
- `/doubledoors locale credits` lists credits for every bundled and custom language file currently available.
- `/doubledoors locale credit &lt;code&gt;` shows credits for one language file.

### /doubledoors debug

- Permission: `doubledoors.debug`
- Sender: player only
- Effect: Toggles debug mode for the player. When enabled, the player sees diagnostic messages about partner matching, location filter blocks, and protection-plugin denials.

### /doubledoors preview

- Permission: `doubledoors.preview`
- Sender: player only
- Effect: Shows preview particles at the linked partner block of the targeted door-like block and prints its location and facing direction. Works for both mirrored doors and recursive gate/trapdoor sets.
- Range: 8 blocks.

### /doubledoors grief villagers

- Permission: `doubledoors.grief`
- Sender: player only
- Effect: Toggles per-GriefPrevention-claim villager linked-door access. When blocked, villagers will not trigger linked opening for doors inside that claim.
- Requires: Player must be standing inside a GriefPrevention claim and have claim-management rights.

## Permission Nodes

- doubledoors.use
- doubledoors.knock
- doubledoors.knock.volume
- doubledoors.autoclose
- doubledoors.iron.manual
- doubledoors.toggle
- doubledoors.toggle.autoclose
- doubledoors.toggle.knock
- doubledoors.reload
- doubledoors.server-toggle
- doubledoors.locale
- doubledoors.grief
- doubledoors.debug
- doubledoors.preview
- doubledoors.update.notify

## Defaults

- doubledoors.use: true
- doubledoors.knock: true
- doubledoors.knock.volume: true
- doubledoors.autoclose: true
- doubledoors.iron.manual: op
- doubledoors.toggle: true
- doubledoors.toggle.autoclose: true
- doubledoors.toggle.knock: true
- doubledoors.reload: op
- doubledoors.server-toggle: op
- doubledoors.locale: true
- doubledoors.grief: true
- doubledoors.debug: op
- doubledoors.preview: true
- doubledoors.update.notify: op

## Suggested LuckPerms Setup

Grant normal users:

- doubledoors.use
- doubledoors.toggle
- doubledoors.knock
- doubledoors.knock.volume
- doubledoors.autoclose
- doubledoors.toggle.autoclose
- doubledoors.toggle.knock
- doubledoors.locale
- doubledoors.grief
- doubledoors.preview

Grant staff/admins:

- doubledoors.reload
- doubledoors.server-toggle
- doubledoors.debug
- doubledoors.update.notify

Grant trusted players (optional):

- doubledoors.iron.manual

## Tab Completion

Main subcommands:

- reload
- toggle
- knock-volume
- server-toggle
- locale
- grief
- debug
- preview

Second-level toggle options:

- doors
- gates
- trapdoors
- autoclose
- knock

Locale subcommands:

- credits
- credit

## Notes

- Door knock sound is allowed regardless of GriefPrevention claim ownership (it is not blocked just because the claim belongs to another player).
- Players who are sneaking (crouching) do not trigger linked door behavior, preventing accidental linked-opening when placing blocks or interacting with items on doors.
- Only main-hand interactions are processed; off-hand interactions are ignored to prevent double-processing.
- Players with the `doubledoors.iron.manual` permission can right-click iron doors to toggle them manually, which is not possible in vanilla Minecraft.
