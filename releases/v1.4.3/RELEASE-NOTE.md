# DoubleDoors v1.4.3 Release Notes

Patch release
Release date: 2026-05-16

## Fixed

- Fixed the Velocity proxy companion failing to load with `No implementation for Logger was bound` after SLF4J was incorrectly relocated in the shaded jar.
- Fixed Paper servers not reliably detecting proxy-side Geyser/Floodgate when the Velocity companion and Bukkit plugin share the same SQL database.

## Changed

- Velocity proxy heartbeats now include explicit Geyser/Floodgate presence flags, and Bukkit refreshes that shared state after SQL startup and periodically while running.

## Breaking Changes

- None.

## Upgrade Guide

1. Back up your server folder and existing plugin data.
2. Replace the old jar(s) with the new release artifact(s).
3. Restart Velocity if you use the proxy companion.
4. Start the server/proxy and confirm DoubleDoors loads without proxy plugin creation errors.

## Artifacts

- Bukkit/Spigot/Paper/Purpur: doubledoors-bukkit-1.4.3.jar
- Optional proxy companion: doubledoors-proxy-1.4.3.jar
