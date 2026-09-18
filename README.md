# Double Doors Server

[![GitHub Release](https://img.shields.io/github/v/release/SzaBee13/double-doors-server)](https://github.com/SzaBee13/double-doors-server/releases)
[![GitHub License](https://img.shields.io/github/license/SzaBee13/double-doors-server)](https://github.com/SzaBee13/double-doors-server/blob/main/LICENSE)
[![GitHub Issues](https://img.shields.io/github/issues/SzaBee13/double-doors-server)](https://github.com/SzaBee13/double-doors-server/issues)
[![Crowdin](https://badges.crowdin.net/double-doors-server/localized.svg)](https://crowdin.com/project/double-doors-server)
<br>
[![Online players](https://img.shields.io/endpoint?url=https%3A%2F%2Ffaststats.dev%2Fapi%2Fshields%2Fdouble-doors-server%3Fmetric%3Donline_players&style=flat)](https://faststats.dev/project/double-doors-server)
[![Online servers](https://img.shields.io/endpoint?url=https%3A%2F%2Ffaststats.dev%2Fapi%2Fshields%2Fdouble-doors-server%3Fmetric%3Donline_servers&style=flat)](https://faststats.dev/project/double-doors-server)
[![Total downloads](https://img.shields.io/endpoint?url=https%3A%2F%2Ffaststats.dev%2Fapi%2Fshields%2Fdouble-doors-server%3Fmetric%3Ddownloads%26label%3DDownloads&style=flat)](https://faststats.dev/project/double-doors-server)

A plugin that opens mirrored double doors together, with low-latency syncing and optional compatibility handling for common server stacks.

## Documentation

For detailed documentation, see the [documentation](https://doubledoors.szabee.me).

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

- Supports Minecraft Java Edition `1.21.x`, `26.1.x`, `26.2.x`, and `26.3.x`.

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

## License

Licensed under the GNU General Public License v3.0.
See [LICENSE](LICENSE).
