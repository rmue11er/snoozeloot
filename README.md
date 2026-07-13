# SnoozeLoot

SnoozeLoot rewards AFK players with **SnoozePoints** (action bar, title, particles) and provides a
config-driven **points shop** GUI with purchase limits, confirmation, and transaction history.

Target: **Paper 1.20+** (tested on 1.20.6).

## Prerequisites

- **JDK 21** (for Minecraft/Paper 1.20.5+)
- **Maven** (`mvn`)

```bash
java -version
mvn -version
```

## Build

```bash
mvn -q -Dmaven.repo.local=.m2 clean package
```

Output jar (shaded, includes SQLite + bStats):

- `target/snoozeloot-1.1.0.jar`

Do not deploy `original-snoozeloot-*.jar` — that artifact excludes bundled dependencies.

## Local dev server

Build and start:

```bash
chmod +x ./dev.sh ./stop.sh ./run/start.sh ./run/stop.sh
./dev.sh
```

Stop the server:

```bash
./stop.sh
```

The server runs from `run/` and copies the plugin jar from `target/` to `run/plugins/SnoozeLoot.jar` on each start.

Defaults:

- Minecraft version: `1.20.6` (override with `MC_VERSION=...`)
- RAM: `2G` (override with `MEMORY=...`)

Example:

```bash
MC_VERSION=1.20.6 MEMORY=4G ./dev.sh
```

## Server installation

1. Stop the Paper server
2. Copy the jar to `plugins/`
3. Start the server

Optional: install [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) and/or WorldGuard for extended features.

## Commands & permissions

| Command | Description | Permission |
|---------|-------------|------------|
| `/snooze` | Show your balance | `snoozeloot.use` |
| `/snooze show <player>` | Show another player's balance | `snoozeloot.use` |
| `/snooze stats` | Leaderboard + your rank | `snoozeloot.use` |
| `/snooze shop` | Open the points shop | `snoozeloot.use` |
| `/snooze help` | Show commands available to you | `snoozeloot.use` |
| `/snooze pay <player> <amount>` | Send SnoozePoints | `snoozeloot.pay` |
| `/snooze history [limit]` | Your purchase history | `snoozeloot.use` |
| `/snooze history <limit>` | All recent purchases (admin) | `snoozeloot.history.admin` |
| `/snooze admin` | Show AFK/payout settings | `snoozeloot.admin` |
| `/snooze give/remove/set/reset` | Manage balances | `snoozeloot.admin` |
| `/snooze reload` | Reload config & messages | `snoozeloot.admin` |
| `/snooze debug [player]` | AFK debug info | `snoozeloot.admin` |

## PlaceholderAPI

When PlaceholderAPI is installed, these placeholders are available:

| Placeholder | Description |
|-------------|-------------|
| `%snoozeloot_points%` | Current SnoozePoints balance |
| `%snoozeloot_rank%` | Leaderboard rank (`-` if unranked) |
| `%snoozeloot_afk%` | Whether the player is currently AFK |
| `%snoozeloot_streak%` | Daily login streak days |

## Configuration

On first start, `plugins/SnoozeLoot/config.yml` is created (`config-version: 3`).

### Storage

```yml
storage:
  type: yaml   # yaml | sqlite
language: en   # loads messages_<language>.yml (en, de)
```

- **yaml**: `players.yml`, `meta.yml`, `transactions.yml`
- **sqlite**: single `snoozeloot.db` (points, meta, purchase counts, transactions)

### AFK & points

- `afk.idle-check-interval-seconds` — activity check interval
- `afk.afk-time-threshold-seconds` — idle time before AFK starts
- `afk.payout-interval-seconds` / `afk.points-per-interval` — earning rate
- `afk.allowed-worlds` / `afk.allowed-regions` — restrict where AFK works (empty = all)
- `afk.blocked-gamemodes` — e.g. CREATIVE, SPECTATOR
- `afk.min-active-seconds-before-afk` — active play required before first AFK
- `afk.max-afk-seconds-per-day` / `max-afk-seconds-per-session` — caps (`-1` = unlimited)
- `afk.pool-detection` — ignore circular pool AFK machines
- `afk.start-notification.*` — title + sound on AFK enter
- `afk.end-notification.*` — sound on AFK exit

### Bonuses & streaks

Daily/weekly bonuses are claimed automatically when entering AFK. Streak multipliers boost AFK earnings when the player has enough active play time.

```yml
bonuses:
  daily:
    enabled: true
    points: 25
  weekly:
    enabled: true
    points: 100
  streak:
    enabled: true
    min-minutes-per-day: 30
    multiplier-per-day: 0.05
    max-multiplier: 1.5
```

### Multipliers

Permission-based multipliers (highest value wins):

```yml
multipliers:
  snoozeloot.multiplier.vip: 1.5
  snoozeloot.multiplier.mvp: 2.0
```

### Pay

```yml
pay:
  enabled: true
  min-amount: 1
  cooldown-seconds: 30
```

### Shop

- `shop.confirm-above-price` — double-click confirmation for expensive items
- `shop.transaction-log-max-entries` — max stored purchases
- Per item: `purchase-limit` (`-1` = unlimited)
- Commands support `%player%`, `%uuid%`, `%world%`, `%points%`

### Integrations

```yml
update-checker:
  enabled: true
  version-url: ""              # optional; empty uses built-in default URL
  notify-permission: snoozeloot.admin

bstats:
  enabled: true                # disable SnoozeLoot metrics only
```

#### bStats metrics

SnoozeLoot uses [bStats](https://bstats.org/) (plugin id **32608**) to collect anonymous usage statistics
(server count, player counts, Java version, and config summaries like storage type or language).
There is no performance impact, and no player names or IPs are sent.

**Opt out (either method works):**

1. **Per plugin** — set `bstats.enabled: false` in `plugins/SnoozeLoot/config.yml`
2. **Server-wide** — set `enabled: false` in `plugins/bStats/config.yml` (created automatically on first start; affects all bStats-enabled plugins on the server)

Custom charts reported: `language`, `storage_type`, `sqlite`, `daily_bonus`, `weekly_bonus`,
`streak_bonus`, `pay_enabled`, `worldguard`, `placeholderapi`.

## Anti-exploit

Movement in water, vehicles, or on rails does not count as activity when configured. Pool detection ignores circular AFK pool movement patterns.

## Tests

```bash
mvn -q -Dmaven.repo.local=.m2 test
```

## MiniMessage

All text in `messages_*.yml` and shop names/lore uses MiniMessage formatting. Use `<placeholder>` tags (not `%placeholder%`) in message files.
