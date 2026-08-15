---
title: Compatibility
---

## Platform

- Bukkit/Spigot/Paper/Purpur 1.21+
- Folia (region-aware scheduling)
- Java 25+

## Permission Plugins

### LuckPerms

- Uses standard Bukkit permissions.
- No special integration code required.
- Manage `doubledoors.*` nodes as usual.

## Protection Plugins

### GriefPrevention

- Optional reflective integration.
- Linked partner door checks claim build permission before toggling.
- If reflection fails or API signatures differ, behavior fails open (no hard break).
- Per-claim villager blocking: admins can toggle villager linked-door access per claim using `/doubledoors grief villagers`.
- Config option `griefprevention.requireBuildForLinkedDoors` controls whether Build trust (true) or Access trust (false) is required.

### WorldGuard

- Optional reflective integration via `softdepend`.
- **Build permission check**: When `worldGuardRespectBuildPermission` is true, linked door actions require WorldGuard build permission.
- **USE flag check**: When `worldGuardRespectUseFlag` is true, linked door actions require the WorldGuard USE flag to be set to `allow`.
- **Custom state-flag**: A configurable flag name (default `double-doors-allow`) can be set per-region to `allow` or `deny` DoubleDoors behavior. Resolved with priority-based flag resolution when multiple regions overlap.
- **Region blacklist/whitelist**: `worldGuardRegionFilter.mode` controls which WorldGuard regions are subject to DoubleDoors linking. Region IDs are collected via WorldGuard's `getApplicableRegions()` API.
- If reflection fails or API signatures differ, behavior fails open (no hard break).

## Bedrock Bridge

### Geyser and Floodgate

- Optional soft dependency.
- Duplicate interaction debounce is active to reduce double-fire toggles for the same block interaction.
- When SQL storage is enabled, the Bukkit plugin polls the shared database to detect if a remote Velocity proxy has Geyser/Floodgate active, enabling Bedrock client debounce even when Geyser runs on a separate proxy.

## Concurrency Frameworks

### Folia

- Plugin declares `folia-supported: true` in `plugin.yml`.
- Uses region-aware scheduling via reflection: `getGlobalRegionScheduler()`, `getRegionScheduler()`, `getAsyncScheduler()`.
- Falls back to standard Bukkit scheduler when Folia is not present.
- Delayed block updates and shared-state access use Folia-aware region scheduling.

## Other Plugins

DoubleDoors is event-driven and generally compatible with typical server stacks. If another plugin cancels interaction events before MONITOR handlers run, DoubleDoors will not process those cancelled events.

## Practical Advice

- Keep only one plugin responsible for advanced multi-block door logic.
- If you stack multiple similar plugins, expect conflicts or race-like state overrides.
- Prefer explicit permission setup for user groups even if defaults already allow use.
