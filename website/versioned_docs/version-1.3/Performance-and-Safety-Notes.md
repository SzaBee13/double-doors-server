---
title: Performance and Safety Notes
id: Performance-and-Safety-Notes
slug: /Performance-and-Safety-Notes
---

## Performance Characteristics

- Double-door partner matching is constant-time with local neighbor checks.
- Recursive group syncing is BFS bounded by max distance (1..32).
- Most hot-path checks are early-return guards.

## Main-Thread Safety

- Event handling and block updates happen on the server thread.
- Disk writes for players.yml are scheduled asynchronously.

## State Consistency Strategy

- State is read after scheduled delay to match final vanilla outcome.
- For mirrored doors, upper and lower halves are explicitly synchronized.
- For recursive sets, block data snapshots are taken before mutation to reduce ordering side effects.

## Tuning Recommendations

- Keep recursiveOpeningMaxBlocksDistance moderate for large builds.
- Disable block families you do not use (doors, fence gates, trapdoors).
- Use serverWideEnabled as an immediate kill switch for incident response.

## Known Tradeoffs

- Reflection-based GriefPrevention hooks are resilient but can miss strict checks on unusual API changes.
- If enableRecursiveOpening is false, linked state application is skipped in all paths under current implementation.
