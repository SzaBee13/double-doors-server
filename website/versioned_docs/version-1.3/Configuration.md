---
title: Configuration
id: Configuration
slug: /Configuration
---

Main file: plugins/DoubleDoors/config.yml

## Full Key Reference

### enableRecursiveOpening

- Type: boolean
- Default: true
- Effect: Master gate for linked synchronization logic.
- Note: In current behavior, when false, linked state application is skipped for player, redstone, and villager paths.

### recursiveOpeningMaxBlocksDistance

- Type: integer
- Default: 10
- Allowed range: 1 to 32
- Effect: BFS depth limit for connected same-type gate/trapdoor searches.
- Internal clamp: values below 1 become 1, above 32 become 32.

### enableDoors

- Type: boolean
- Default: true
- Effect: Enables door handling when a block type name ends with _DOOR.

### enableFenceGates

- Type: boolean
- Default: true
- Effect: Enables fence-gate handling when type ends with _FENCE_GATE.

### enableTrapdoors

- Type: boolean
- Default: true
- Effect: Enables trapdoor handling when type ends with _TRAPDOOR.

### enableVillagerLinkedDoors

- Type: boolean
- Default: true
- Effect: Enables villager-driven open and close synchronization.

### serverWideEnabled

- Type: boolean
- Default: true
- Effect: Global kill switch for all plugin behavior.
- Runtime toggle command: /doubledoors server-toggle

## Example Config

```yaml
enableRecursiveOpening: true
recursiveOpeningMaxBlocksDistance: 10
enableDoors: true
enableFenceGates: true
enableTrapdoors: true
enableVillagerLinkedDoors: true
serverWideEnabled: true
```

## Reload Behavior

Use /doubledoors reload to reload:

- config.yml
- players.yml cache

No full server restart is required for normal config edits.
