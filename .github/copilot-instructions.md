# GitHub Copilot — Workspace Instructions

## Project overview
**DoubleDoors** is a Paper/Spigot 1.21 plugin written in Java 25.  
It links adjacent doors, fence gates, and trapdoors so they open and close simultaneously — for players, redstone, and villager AI.

## Build
```
./gradlew build
```
The shaded JARs are produced at `bukkit/build/libs/doubledoors-bukkit-<version>.jar` and `velocity/build/libs/doubledoors-velocity-<version>.jar`.

## Conventions

### Java style
- Java 25 features are encouraged: records, sealed classes, pattern-matching instanceof, switch expressions.
- All public API methods must have a Javadoc comment.
- `final` on classes and fields unless mutability is required.
- No wildcard imports.

### Package layout
| Package | Responsibility |
|---|---|
| `me.szabee.doubledoors` | Plugin main class + command/tab handling |
| `me.szabee.doubledoors.config` | `PluginConfig` (server YAML), `PlayerPreferences` (per-player YAML) |
| `me.szabee.doubledoors.listeners` | `DoorInteractListener` (player clicks), `RedstoneListener` (redstone + villager) |
| `me.szabee.doubledoors.util` | `DoorUtil` (block search), `ProtectionCompat` (GriefPrevention reflection) |

### Configuration files
| File | Purpose |
|---|---|
| `src/main/resources/config.yml` | Server-wide defaults. Always update when adding new config keys. |
| `plugins/DoubleDoors/players.yml` (runtime) | Per-player preferences written by `PlayerPreferences`. |

### Important rules
- **Never** read block data inside an event handler at `MONITOR` priority — always schedule a `runTaskLater` so vanilla has settled the state first.  
  - Player interactions: 1-tick delay.  
  - Villager AI events: 2-tick delay (`VILLAGER_DELAY_TICKS = 2L`).
- Protection integrations (GriefPrevention) are invoked via reflection in `ProtectionCompat`; keep them fault-tolerant — always fail open on `ReflectiveOperationException`.
- Per-player preferences must be persisted asynchronously (`runTaskAsynchronously`) after every mutation so the main thread is never blocked by disk I/O.

### Adding a new block type
1. The `isEnabledType` static helper in `DoorInteractListener` checks `Material.name()` suffixes (`_DOOR`, `_FENCE_GATE`, `_TRAPDOOR`).  
2. Add a matching config key in `config.yml`, `PluginConfig`, and `PlayerPreferences` if per-player control is desired.

### Villager support
`RedstoneListener` listens to both `EntityInteractEvent` (open direction) and `EntityChangeBlockEvent` (close direction).  
Both are guarded by `config.isEnableVillagerLinkedDoors()`.

## Localization

**Translation files are only updated for `en_US` by default.** When adding new message keys or modifying existing ones, update only `src/main/resources/lang/defaults.json` and `src/main/resources/lang/english/en_US.json`.  
Other regional locales (en_GB, fr_FR, de_DE, etc.) will inherit the English US text as a fallback until someone explicitly requests localization support.  
If the user asks to localize a new feature to other languages, then propagate the changes across all regional variants.

## Testing
There are automated unit tests in `bukkit` and `velocity`; run `./gradlew build` first.  
Then test manually on a local Paper 1.21 server for double doors, double fence gates, double trapdoors, redstone coupling, villager pathfinding, and GriefPrevention claim boundaries.

## Source control (Git)
- Use feature branches for new features and bug fixes; merge to `main` when complete.
- Write clear commit messages describing the change and motivation.
- Avoid large, monolithic commits; break changes into logical steps when possible.
- Use pull requests for code review and discussion before merging to `main`.
- Regularly pull from `main` to keep branches up to date and resolve conflicts early.
- Tag releases with semantic versioning (e.g., `v1.0.0`) and include release notes summarizing changes.
