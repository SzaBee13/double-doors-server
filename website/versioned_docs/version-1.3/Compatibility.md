---
title: Compatibility
id: Compatibility
slug: /Compatibility
---

## Platform

- Bukkit/Spigot/Paper/Purpur 26.1+
- Java 25

## Permission Plugins

### LuckPerms

- Uses standard Bukkit permissions.
- No special integration code required.
- Manage doubledoors.* nodes as usual.

## Protection Plugins

### GriefPrevention

- Optional reflective integration.
- Linked partner door checks claim build permission before toggling.
- If reflection fails or API signatures differ, behavior fails open (no hard break).

## Bedrock Bridge

### Geyser and Floodgate

- Optional soft dependency.
- Duplicate interaction debounce is active to reduce double-fire toggles for the same block interaction window.

## Other Plugins

DoubleDoors is event-driven and generally compatible with typical server stacks. If another plugin cancels interaction events before MONITOR handlers run, DoubleDoors will not process those cancelled events.

## Practical Advice

- Keep only one plugin responsible for advanced multi-block door logic.
- If you stack multiple similar plugins, expect conflicts or race-like state overrides.
- Prefer explicit permission setup for user groups even if defaults already allow use.
