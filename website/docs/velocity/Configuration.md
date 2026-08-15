---
title: Configuration
---

Configuration is stored in `plugins/DoubleDoors/proxy-config.properties`.

```properties
sql.enabled=true
sql.jdbcUrl=jdbc:sqlite:plugins/DoubleDoors/doubledoors.db
sql.username=
sql.password=
sql.proxyId=velocity-main
sql.heartbeatSeconds=30
```

## Options

| Option | Default | Description |
| --- | --- | --- |
| `sql.enabled` | `false` | Enables proxy heartbeat reporting when Geyser or Floodgate is detected. |
| `sql.jdbcUrl` | SQLite URL | SQLite or MySQL JDBC connection URL. |
| `sql.username` | empty | Database username; leave empty for SQLite. |
| `sql.password` | empty | Database password; leave empty for SQLite. |
| `sql.proxyId` | `velocity-main` | Unique identifier for this proxy. |
| `sql.heartbeatSeconds` | `30` | Heartbeat interval; minimum 5 seconds. |

Restart Velocity after changing this file. See [Proxy Setup](./Proxy-Setup) for SQLite and MySQL examples.
