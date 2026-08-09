---
title: How It Works
id: How-It-Works
slug: /How-It-Works
---

## Event Model

The plugin listens at MONITOR priority with ignoreCancelled true for:

- PlayerInteractEvent
- BlockRedstoneEvent
- EntityInteractEvent (villager open path)
- EntityChangeBlockEvent (villager close path)

## Why Delays Exist

At MONITOR priority, reading block state immediately can capture pre-final state.
To mirror the final vanilla state safely:

- Player path uses next tick scheduling (1 tick).
- Redstone path uses 1 tick delay.
- Villager path uses 2 tick delay.

## Door Partner Matching

For standard double doors, partner detection requires:

- Same material
- Same facing direction
- Opposite hinge
- Side-by-side left or right

Upper-half clicks are normalized to lower-half door blocks before matching.

## Recursive Group Sync

For fence gates and trapdoors, connected same-material blocks are found with BFS using 6-direction adjacency:

- +X, -X, +Y, -Y, +Z, -Z

Depth is limited by recursiveOpeningMaxBlocksDistance.

## Player Preference Layer

Each player can toggle:

- Global linked behavior
- Doors only
- Fence gates only
- Trapdoors only

Data is cached in memory and persisted asynchronously to players.yml after each mutation.

## Server-Wide Layer

The serverWideEnabled config key acts as a global runtime switch.
When false, handlers return early.

## Protection Check Layer

When a mirrored partner door is about to be toggled by a player, GriefPrevention checks are attempted via reflection. If the check cannot be resolved, logic fails open to avoid breaking gameplay.
