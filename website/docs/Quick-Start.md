---
title: Quick Start
id: Quick-Start
slug: /Quick-Start
---

This page is optimized for first-time setup.

## 1) Basic Server Setup

1. Install the plugin.
2. Keep all default settings in config.yml for initial validation.
3. Ensure players have doubledoors.use (default true).

## 2) Test Cases

Run these tests in order:

1. Double doors

   - Place mirrored same-type double doors.
   - Open one side as a player.
   - Both sides should match.

2. Fence gates
   - Place connected fence gates of the same material.
   - Open one gate.
   - Connected gates should follow when recursive opening is enabled.

3. Trapdoors
   - Place connected trapdoors of the same material.
   - Open one trapdoor.
   - Connected trapdoors should follow when recursive opening is enabled.

4. Redstone
   - Power one supported block.
   - Confirm linked blocks synchronize to the resulting state.

5. Villager behavior
   - Let villagers path through doors.
   - Confirm both open and close behavior synchronize if villager support is enabled.

## 3) Essential Admin Commands

- /doubledoors reload
- /doubledoors server-toggle

## 4) Optional Player Controls

- /doubledoors toggle
- /doubledoors toggle doors
- /doubledoors toggle gates
- /doubledoors toggle trapdoors

## 5) Recommended Defaults

- Keep enableRecursiveOpening true for synchronized behavior.
- Keep recursiveOpeningMaxBlocksDistance moderate (8-12).
- Keep enableVillagerLinkedDoors true unless villagers cause unwanted toggles.
