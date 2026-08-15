---
title: Velocity Troubleshooting
---

## Plugin Does Not Load

- Check that the jar is in the Velocity `plugins/` directory, not a backend `plugins/` directory.
- Verify the proxy is running Java 21 or newer.
- Read the first startup error and resolve dependency or version mismatches before changing SQL settings.

## Geyser or Floodgate Is Not Detected

Install Geyser-Velocity or Floodgate on the same proxy and ensure it loads before DoubleDoors. The detection message is expected when neither bridge is installed; heartbeat reporting remains disabled in that case.

## No Heartbeat Row

- Set `sql.enabled=true`.
- Check the JDBC URL, credentials, and database permissions.
- Use a unique `sql.proxyId` for each proxy.
- For SQLite, ensure `plugins/DoubleDoors/` is writable.
- For MySQL, verify connectivity from the proxy host and that the database user can create tables.

## Backend Doors Are Not Synchronized

The Velocity plugin does not synchronize door blocks across independent backend servers. Install the Bukkit plugin on each backend and use a shared world or backend-specific configuration as appropriate.
