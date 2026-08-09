---
title: FAQ
id: FAQ
slug: /FAQ
---

## Does this support Paper only, or Spigot too?

It targets the Bukkit/Paper API level for 26.1+ and is intended for Paper/Spigot server environments.

## Can players opt out individually?

Yes. Players can toggle all behavior or specific block families with /doubledoors toggle subcommands.

## Can I disable all behavior quickly during an incident?

Yes. Use /doubledoors server-toggle to flip the global runtime switch.

## Why are there delayed sync tasks?

Delays ensure the plugin reads final post-vanilla block state rather than pre-update state when running at MONITOR priority.

## Is GriefPrevention required?

No. It is optional. If present, linked partner checks are attempted through reflection.

## Is recursive opening only for gates and trapdoors?

Recursive BFS grouping is used for non-door openables. Under current implementation, the recursive feature flag also gates linked state application in all paths.

## Where are player preferences stored?

In plugins/DoubleDoors/players.yml.

## Can I edit players.yml while server is running?

You can, but best practice is to use in-game commands and then /doubledoors reload for consistency.
