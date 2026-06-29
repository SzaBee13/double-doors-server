# Double Doors Server

[![GitHub Release](https://img.shields.io/github/v/release/SzaBee13/double-doors-spigot)](https://github.com/SzaBee13/double-doors-spigot/releases)
[![GitHub License](https://img.shields.io/github/license/SzaBee13/double-doors-spigot)](https://github.com/SzaBee13/double-doors-spigot/blob/main/LICENSE)
[![GitHub Issues](https://img.shields.io/github/issues/SzaBee13/double-doors-spigot)](https://github.com/SzaBee13/double-doors-spigot/issues)
[![Modrinth Game Versions](https://img.shields.io/modrinth/game-versions/double-doors-server)](https://modrinth.com/plugin/double-doors-server)
[![Crowdin](https://badges.crowdin.net/double-doors-server/localized.svg)](https://crowdin.com/project/double-doors-server)
<br>
[![Online players](https://img.shields.io/endpoint?url=https%3A%2F%2Ffaststats.dev%2Fapi%2Fshields%2Fdouble-doors-server%3Fmetric%3Donline_players&style=flat)](https://faststats.dev/project/double-doors-server)
[![Online servers](https://img.shields.io/endpoint?url=https%3A%2F%2Ffaststats.dev%2Fapi%2Fshields%2Fdouble-doors-server%3Fmetric%3Donline_servers&style=flat)](https://faststats.dev/project/double-doors-server)

A Bukkit/Spigot plugin that opens mirrored double doors together, with low-latency syncing and optional compatibility handling for common server stacks.

## Features

- Same-tick partner door sync (no scheduled 1-tick delay)
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

## Official sources

- [GitHub *www.github.com/SzaBee13/double-doors-server*](https://github.com/SzaBee13/double-doors-server)
- [Modrinth *www.modrinth.com/plugin/double-doors-server*](https://modrinth.com/plugin/double-doors-server)
- [Hangar *hangar.papermc.io/SzaBee13/double-doors-server*](https://hangar.papermc.io/SzaBee13/double-doors-server)
- [CurseForge *www.curseforge.com/minecraft/bukkit-plugins/double-doors-server*](https://www.curseforge.com/minecraft/bukkit-plugins/double-doors-server)

## Compatibility

- Supports Minecraft Java Edition `1.21.X`, `26.1.X` and `26.2.X`.
### Geyser / Floodgate

- Plugin declares soft-depends on `Geyser-Spigot` and `floodgate`.
- A short duplicate-interaction debounce window is used to avoid rapid duplicate toggles on the same block.

### LuckPerms

- Works through standard Bukkit permissions, so LuckPerms applies automatically.
- Use `doubledoors.use` to allow/deny linked opening behavior.

### GriefPrevention

- Plugin declares a soft-depend on `GriefPrevention`.
- When present, linked-door interaction is checked against claim build permission before toggling the partner door.

## Proxy Setup (Multi-Server)

DoubleDoors includes an optional **Velocity proxy plugin** for Geyser/Floodgate environments with multiple backend servers.

### Proxy Features

- Shared SQL heartbeat/presence tracking across multiple proxies
- Automatic detection of Geyser/Floodgate clients
- Support for SQLite and MySQL databases
- Connection pooling via HikariCP for efficient SQL resource usage

### Proxy Installation

1. Download the proxy JAR from the releases page (`doubledoors-proxy-<version>.jar`)
2. Place it in your Velocity `plugins/` directory
3. Restart the proxy
4. A `plugins/DoubleDoors/proxy-config.properties` file will be generated

### Proxy Configuration

Edit `plugins/DoubleDoors/proxy-config.properties`

For more, visit the [wiki](https://github.com/SzaBee13/double-doors-server/wiki/Proxy-Setup)

## Commands

- `/doubledoors reload` - reload config
- `/doubledoors toggle` - toggle behavior for yourself

Alias: `dd`

Check out more commands at the [wiki](https://github.com/SzaBee13/double-doors-server/wiki/Commands-and-Permissions)

## Permissions

- `doubledoors.use` (default: `true`)
- `doubledoors.toggle` (default: `true`)
- `doubledoors.reload` (default: `op`)

Check out more permissions at the [wiki](https://github.com/SzaBee13/double-doors-server/wiki/Commands-and-Permissions)

## Config

`src/main/resources/config.yml`

- `enableRecursiveOpening` (default: `true`)
- `recursiveOpeningMaxBlocksDistance` (default: `10`)
- `enableDoors` (default: `true`)
- `enableFenceGates` (default: `true`)
- `enableTrapdoors` (default: `true`)
- `enableVillagerLinkedDoors` (default: `true`)
- `serverWideEnabled` (default: `true`)
- `language` (default: `en_US`)

Language files:

- Built-in fallback file: `src/main/resources/lang/en_US.json`
- Runtime custom language folder: `plugins/DoubleDoors/lang/`
- Set active language with `language: <code>` in `config.yml` (example: `language: de_DE`)
- Custom files are JSON objects of key/value strings and override built-in messages when present.

## License

Licensed under the GNU General Public License v3.0.
See `LICENSE`.
