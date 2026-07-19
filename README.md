# Double Doors Server

[![GitHub Release](https://img.shields.io/github/v/release/SzaBee13/double-doors-server)](https://github.com/SzaBee13/double-doors-server/releases)
[![GitHub License](https://img.shields.io/github/license/SzaBee13/double-doors-server)](https://github.com/SzaBee13/double-doors-server/blob/main/LICENSE)
[![GitHub Issues](https://img.shields.io/github/issues/SzaBee13/double-doors-server)](https://github.com/SzaBee13/double-doors-server/issues)
[![Modrinth Game Versions](https://img.shields.io/modrinth/game-versions/double-doors-server)](https://modrinth.com/plugin/double-doors-server)
[![Crowdin](https://badges.crowdin.net/double-doors-server/localized.svg)](https://crowdin.com/project/double-doors-server)
<br>
[![Online players](https://img.shields.io/endpoint?url=https%3A%2F%2Ffaststats.dev%2Fapi%2Fshields%2Fdouble-doors-server%3Fmetric%3Donline_players&style=flat)](https://faststats.dev/project/double-doors-server)
[![Online servers](https://img.shields.io/endpoint?url=https%3A%2F%2Ffaststats.dev%2Fapi%2Fshields%2Fdouble-doors-server%3Fmetric%3Donline_servers&style=flat)](https://faststats.dev/project/double-doors-server)
[![Total downloads](https://img.shields.io/endpoint?url=https%3A%2F%2Ffaststats.dev%2Fapi%2Fshields%2Fdouble-doors-server%3Fmetric%3Ddownloads%26label%3DDownloads&style=flat)](https://faststats.dev/project/double-doors-server)

A Bukkit/Spigot plugin that opens mirrored double doors together, with low-latency syncing and optional compatibility handling for common server stacks.

## Features

- Same-tick partner door sync
- Strict mirrored pair matching for doors:
  - same door type
  - same facing direction
  - opposite hinge
  - side-by-side only
- Optional recursive opening support for non-door openables (fence gates/trapdoors)
- Per-player toggle: `/doubledoors toggle`
- LuckPerms-friendly permission nodes
- GriefPrevention compatibility check for linked-door claim access
- Duplicate interaction debounce (helps packet duplication patterns seen with some Bedrock/Geyser flows)
- Translation support via JSON language files (built-in + custom)

## Compatibility

- Supports Minecraft Java Edition `1.21.x`, `26.1.x`, and `26.2.x`.

### Geyser / Floodgate

- Plugin declares soft-depends on `Geyser-Spigot` and `floodgate`.
- A short duplicate-interaction debounce window is used to avoid rapid duplicate toggles on the same block.

### LuckPerms

- Works through standard Bukkit permissions, so LuckPerms applies automatically.
- Use `doubledoors.use` to allow/deny linked opening behavior.

### GriefPrevention

- Plugin declares a soft-depend on `GriefPrevention`.
- When present, linked-door interaction is checked against claim build permission before toggling the partner door.

### Folia

- Folia is supported with region-aware scheduling for delayed block updates and shared-state access.

### Platform Artifacts

- Use `doubledoors-paper-<version>.jar` on Paper, Purpur, or Folia. Paper downloads its SQL libraries when the plugin starts.
- Use `doubledoors-bukkit-<version>.jar` on Bukkit or Spigot. It includes all runtime libraries.

## Proxy Setup (Multi-Server)

DoubleDoors includes an optional **Velocity proxy plugin** for Geyser/Floodgate environments with multiple backend servers.

### Proxy Features

- Shared SQL heartbeat/presence tracking across multiple proxies
- Automatic detection of Geyser/Floodgate clients
- Support for SQLite and MySQL databases
- Connection pooling via HikariCP for efficient SQL resource usage

### Proxy Installation

1. Download the Velocity JAR from the releases page (`doubledoors-velocity-<version>.jar`)
2. Place it in your Velocity `plugins/` directory
3. Restart the proxy
4. A `plugins/DoubleDoors/proxy-config.properties` file will be generated

### Proxy Configuration

Edit `plugins/DoubleDoors/proxy-config.properties`:

```properties
# Enable SQL heartbeat reporting (requires Geyser/Floodgate on this proxy)
sql.enabled=false

# JDBC URL for the shared database
# SQLite example: jdbc:sqlite:plugins/DoubleDoors/doubledoors.db
# MySQL example: jdbc:mysql://localhost:3306/doubledoors
sql.jdbcUrl=jdbc:sqlite:plugins/DoubleDoors/doubledoors.db

# SQL authentication (leave blank for SQLite)
sql.username=
sql.password=

# Unique proxy identifier (for multi-proxy setups)
sql.proxyId=velocity-main

# Heartbeat interval in seconds (minimum 5 seconds)
sql.heartbeatSeconds=30
```

### Multi-Proxy Example

For a setup with multiple Velocity proxies reporting to a shared MySQL database:

**Proxy 1:**
```properties
sql.enabled=true
sql.jdbcUrl=jdbc:mysql://db.example.com:3306/doubledoors
sql.username=dd_user
sql.password=dd_pass
sql.proxyId=velocity-us
sql.heartbeatSeconds=30
```

**Proxy 2:**
```properties
sql.enabled=true
sql.jdbcUrl=jdbc:mysql://db.example.com:3306/doubledoors
sql.username=dd_user
sql.password=dd_pass
sql.proxyId=velocity-eu
sql.heartbeatSeconds=30
```

## Commands

- `/doubledoors reload` - reload config
- `/doubledoors toggle` - toggle behavior for yourself
- `/doubledoors knock-volume <0-1>` - set personal knock sound volume
- `/doubledoors server-toggle` - toggle behavior server-wide
- `/doubledoors locale [code|credits|credit <code>]` - view or set your locale

## Permissions

- `doubledoors.use` (default: `true`)
- `doubledoors.knock` (default: `true`)
- `doubledoors.knock.volume` (default: `true`)
- `doubledoors.autoclose` (default: `true`)
- `doubledoors.iron.manual` (default: `op`)
- `doubledoors.toggle` (default: `true`)
- `doubledoors.toggle.autoclose` (default: `true`)
- `doubledoors.toggle.knock` (default: `true`)
- `doubledoors.locale` (default: `true`)
- `doubledoors.reload` (default: `op`)
- `doubledoors.server-toggle` (default: `op`)
- `doubledoors.grief` (default: `true`)
- `doubledoors.debug` (default: `op`)
- `doubledoors.preview` (default: `true`)
- `doubledoors.update.notify` (default: `op`)

## Config

`bukkit/src/main/resources/config.yml`

- `enableRecursiveOpening` (default: `true`)
- `recursiveOpeningMaxBlocksDistance` (default: `10`)
- `enableDoors` (default: `true`)
- `enableFenceGates` (default: `true`)
- `enableTrapdoors` (default: `true`)
- `enableVillagerLinkedDoors` (default: `true`)
- `serverWideEnabled` (default: `true`)
- `perPlayerLocaleEnabled` (default: `true`)
- `language` (default: `en_US`)

### Update Checker

- Built-in update checks are controlled by `updateChecker.enabled` in `config.yml`.
- If a standalone `PluginUpdater` plugin is installed, DoubleDoors automatically disables its own built-in checks to avoid duplicate notifications.
- Startup logs explicitly state whether update checks are:
  - enabled via DoubleDoors built-in updater,
  - disabled by config,
  - or delegated to standalone `PluginUpdater`.
- When delegating to standalone `PluginUpdater`, configure that plugin to include DoubleDoors update notifications.

Troubleshooting:

- If you do not receive update notifications, first check startup logs to see which updater path is active.
- If built-in checks are active, verify `updateChecker.enabled` and related notify/schedule settings.
- If checks are delegated, verify standalone `PluginUpdater` configuration and permissions for who should receive update notices.

Language files:

- Built-in fallback file: `src/main/resources/lang/en_US.json`
- Runtime custom language folder: `plugins/DoubleDoors/lang/`
- Set active language with `language: <code>` in `config.yml` (example: `language: de_DE`)
- Custom files are JSON objects of key/value strings and override built-in messages when present.

## Build

Requirements:

- Java 25+
- Gradle

Build command:

```bash
./gradlew build
```

Output jars are generated under `bukkit/build/libs/` and `velocity/build/libs/`.

## License

Licensed under the GNU General Public License v3.0.
See [LICENSE](LICENSE).
