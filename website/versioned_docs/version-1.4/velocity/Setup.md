---
title: Setup
---

This page explains how to set up the **DoubleDoors Velocity plugin** for multi-server environments with Geyser/Floodgate.

## When to Use the Proxy Plugin

The DoubleDoors Velocity plugin is useful in these scenarios:

- **Multi-proxy Bedrock network**: Running multiple Velocity proxies for redundancy or geographic distribution
- **Shared door state**: Ensuring door interactions are consistent across backend servers
- **Centralized monitoring**: Tracking proxy presence and health via a shared database

For single-proxy or Java-only setups, the Bukkit/Spigot plugin alone is sufficient.

## Prerequisites

- **Velocity** 3.4.0+
- **Geyser-Velocity** (installed and detected)
- **SQLite** or **MySQL** database for heartbeat storage
- Java 21+ runtime

## Installation Steps

### 1. Download the Plugin

Download `doubledoors-velocity-&lt;version&gt;.jar` from the [releases page](https://github.com/SzaBee13/double-doors-server/releases).

### 2. Install to Velocity

Place the jar in your Velocity `plugins/` directory:

```bash
cp doubledoors-velocity-x.y.z.jar /path/to/velocity/plugins/
```

### 3. Restart Velocity

```bash
# Restart your Velocity instance
# Check console logs for plugin initialization
```

On first startup, the plugin generates:
- `plugins/DoubleDoors/proxy-config.properties` (configuration file)

## Configuration

### Using SQLite (Single Proxy or Local Testing)

**File**: `plugins/DoubleDoors/proxy-config.properties`

```properties
# Enable if Geyser/Floodgate is detected on this proxy
sql.enabled=true

# SQLite database file (relative to current directory)
sql.jdbcUrl=jdbc:sqlite:plugins/DoubleDoors/doubledoors.db

# SQLite has no authentication
sql.username=
sql.password=

# Unique identifier for this proxy
sql.proxyId=velocity-main

# Heartbeat interval in seconds (minimum 5)
sql.heartbeatSeconds=30
```

### Using MySQL (Multi-Proxy Setup)

For multiple proxies sharing a central MySQL database:

**Database Setup**:
```sql
CREATE DATABASE IF NOT EXISTS doubledoors;
CREATE USER IF NOT EXISTS 'dd_user'@'localhost' IDENTIFIED BY 'dd_password';
GRANT ALL PRIVILEGES ON doubledoors.* TO 'dd_user'@'localhost';
FLUSH PRIVILEGES;
```

**Proxy Configuration**:
```properties
sql.enabled=true

# MySQL JDBC URL
sql.jdbcUrl=jdbc:mysql://db.example.com:3306/doubledoors

# MySQL credentials
sql.username=dd_user
sql.password=dd_password

# Unique identifier for each proxy
sql.proxyId=velocity-us-west

# Heartbeat interval
sql.heartbeatSeconds=30
```

### Configuration Options

| Option | Default | Description |
|--------|---------|-------------|
| `sql.enabled` | `false` | Enable proxy heartbeat reporting (requires Geyser/Floodgate) |
| `sql.jdbcUrl` | `jdbc:sqlite:...` | JDBC connection URL (SQLite or MySQL) |
| `sql.username` | _(empty)_ | SQL username (leave empty for SQLite) |
| `sql.password` | _(empty)_ | SQL password (leave empty for SQLite) |
| `sql.proxyId` | `velocity-main` | Unique proxy identifier across your network |
| `sql.heartbeatSeconds` | `30` | Write heartbeat every N seconds (min: 5) |

## Multi-Proxy Example

### Setup: Two Geographically Distributed Proxies

**Proxy 1** (US Region):

```properties
sql.enabled=true
sql.jdbcUrl=jdbc:mysql://central-db.internal:3306/doubledoors
sql.username=dd_user
sql.password=dd_secure_password
sql.proxyId=velocity-us-east
sql.heartbeatSeconds=30
```

**Proxy 2** (EU Region):

```properties
sql.enabled=true
sql.jdbcUrl=jdbc:mysql://central-db.internal:3306/doubledoors
sql.username=dd_user
sql.password=dd_secure_password
sql.proxyId=velocity-eu-west
sql.heartbeatSeconds=30
```

Both proxies write their heartbeat to the same central MySQL database. This allows backend Bukkit servers to query proxy availability via shared SQL.

## Database Schema

The proxy plugin automatically creates the required table:

```sql
CREATE TABLE IF NOT EXISTS dd_proxy_presence (
  proxy_id VARCHAR(128) PRIMARY KEY,
  platform VARCHAR(32) NOT NULL,
  last_seen_epoch_ms BIGINT NOT NULL
);
```

Each proxy updates its row every `heartbeatSeconds`.

## Monitoring & Troubleshooting

### Check Proxy Status

Connect to the database and query:

```sql
SELECT proxy_id, platform, last_seen_epoch_ms FROM dd_proxy_presence;
```

### Plugin Not Detecting Geyser/Floodgate

The proxy plugin only enables heartbeat if Geyser or Floodgate is detected. Check Velocity console:

```
[09:15:42] [Velocity-Netty-Boss-1-1/INFO]: DoubleDoorsProxy did not detect Geyser/Floodgate on this proxy.
```

Ensure these plugins are installed and loading before DoubleDoors Velocity.

### SQL Connection Failures

Check logs for errors like:

```
DoubleDoorsVelocity could not initialize SQL heartbeat: [error message]
```

Common causes:

- **Wrong JDBC URL** - Check database hostname, port, and name
- **Authentication failed** - Verify username/password are correct
- **Network unreachable** - Ensure database is accessible from proxy machine
- **SQLite file permissions** - Ensure `plugins/DoubleDoors/` is writable

### Performance Tuning

The proxy uses **HikariCP** for efficient connection pooling:

- **Max connections**: 5 (configurable in code)
- **Min idle**: 1
- **Connection timeout**: 10 seconds
- **Idle timeout**: 10 minutes
- **Max lifetime**: 30 minutes

For high-traffic proxies, consider increasing `sql.heartbeatSeconds` to reduce database writes.

## Integration with Backend Servers

The shared SQL database allows backend Bukkit servers to:

1. **Query proxy status** - Check which proxies are online
2. **Coordinate features** - Synchronize door state across servers (future feature)
3. **Monitor health** - Alert if a proxy hasn't sent a heartbeat recently

Backend DoubleDoors plugins can query `dd_proxy_presence` table to:

```sql
-- Find online proxies (heartbeat in last 2 minutes)
SELECT proxy_id, platform FROM dd_proxy_presence
WHERE last_seen_epoch_ms &gt; UNIX_TIMESTAMP() * 1000 - 120000;
```

## See Also

- [Installation](./Installation) - Velocity plugin setup
- [Configuration](./Configuration) - Velocity plugin config options
- [Compatibility](./Compatibility) - Velocity compatibility matrix
