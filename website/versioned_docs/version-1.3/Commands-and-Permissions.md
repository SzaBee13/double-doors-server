---
title: Commands and Permissions
id: Commands-and-Permissions
slug: /Commands-and-Permissions
---

## Commands

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

### /doubledoors server-toggle

- Permission: doubledoors.server-toggle
- Sender: console or player
- Effect: Flips serverWideEnabled and persists to config.

## Permission Nodes

- doubledoors.use
- doubledoors.toggle
- doubledoors.reload
- doubledoors.server-toggle

## Defaults

- doubledoors.use: true
- doubledoors.toggle: true
- doubledoors.reload: op
- doubledoors.server-toggle: op

## Suggested LuckPerms Setup

Grant normal users:

- doubledoors.use
- doubledoors.toggle

Grant staff/admins:

- doubledoors.reload
- doubledoors.server-toggle

## Tab Completion

Main subcommands:

- reload
- toggle
- server-toggle

Second-level toggle options:

- doors
- gates
- trapdoors
