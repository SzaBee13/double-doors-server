---
title: Velocity Compatibility
---

## Supported Stack

- Velocity 3.4.0+.
- Java 21+ for the proxy plugin.
- Geyser-Velocity and Floodgate are optional, but required for Bedrock-aware proxy detection.
- SQLite and MySQL are supported for heartbeat storage.

The Velocity plugin is separate from the Bukkit plugin. Backend door behavior depends on the Bukkit-compatible platform running on each server.

## Geyser and Floodgate

DoubleDoors checks whether Geyser or Floodgate is present on the proxy before enabling proxy heartbeat reporting. In a multi-proxy setup, each proxy must have the relevant bridge installed and loaded before DoubleDoors.

## Database Compatibility

- Use SQLite for a single proxy or local testing.
- Use one shared MySQL database for multiple proxies.
- Keep a unique `sql.proxyId` on every proxy.
- Ensure the proxy process can create or write the SQLite file, or reach the MySQL host.

## Backend Compatibility

Install and configure the Bukkit plugin on every backend that uses DoubleDoors. The Velocity plugin reports proxy presence; it does not synchronize block state between separate Minecraft servers.
