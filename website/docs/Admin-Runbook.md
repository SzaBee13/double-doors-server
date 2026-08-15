---
unlisted: true
title: Admin Runbook
id: Admin-Runbook
slug: /Admin-Runbook
---

## Daily Ops

1. Monitor for permission complaints around doubledoors.use.
2. Validate no conflicting door automation plugin changes were introduced.
3. Spot-check villager-heavy areas if villager syncing is enabled.

## Change Management

Before changing config:

1. Snapshot current config.yml.
2. Apply small changes.
3. Run /doubledoors reload.
4. Validate each affected scenario.

## Recommended Validation Matrix

1. Player manually opens mirrored doors.
2. Player manually opens connected gates.
3. Player manually opens connected trapdoors.
4. Redstone toggles each enabled family.
5. Villager opens and closes doors.
6. Claimed-area behavior with GriefPrevention.

## Incident Response

If door behavior causes disruption:

1. Run /doubledoors server-toggle.
2. Confirm serverWideEnabled is now false.
3. Isolate conflicting plugins or recent config changes.
4. Re-enable only after targeted retesting.

## Permission Baseline

- Players: doubledoors.use, doubledoors.toggle
- Staff: doubledoors.reload
- Admins: doubledoors.server-toggle

## Performance Baseline

- Keep recursiveOpeningMaxBlocksDistance conservative unless your use case demands larger components.
- Disable unused families to reduce event-path work.

## Backup and Recovery

- Back up plugins/DoubleDoors/config.yml and plugins/DoubleDoors/players.yml.
- During recovery, restore known-good files and run /doubledoors reload.
