<p align="center">
  <img src="https://raw.githubusercontent.com/rmue11er/snoozeloot/main/docs/summary/logo.png" alt="SnoozeLoot" width="280">
</p>

SnoozeLoot rewards idle players with **SnoozePoints** while they AFK — and lets them spend those points in a fully configurable shop.

No Vault or economy plugin needed. You define what rewards cost and what commands run on purchase (ranks, items, kits, permissions).

<p align="center">
  <img src="https://raw.githubusercontent.com/rmue11er/snoozeloot/main/docs/summary/afk-ingame.png" alt="AFK — action bar and particles" width="640">
</p>

---

## 🚀 Quick start

1. Drop the jar in `plugins/` and start the server (Paper **1.20.5+**, Java **21+**).
2. Edit `plugins/SnoozeLoot/config.yml` — set AFK timers, shop items, rewards.
3. `/snooze reload` (or restart). Players use `/snooze` and `/snooze shop`.

Optional: install **PlaceholderAPI** and/or **WorldGuard** — SnoozeLoot detects them automatically.

---

## 🛏️ How AFK works

1. Player stands still long enough → AFK mode starts (title, sound, Z particles).
2. While AFK: action bar shows balance and earn rate, points pay out on a timer.
3. Player moves again → AFK ends, welcome-back message shows session earnings.

Tune in `config.yml` under `afk:`:

| Setting | What it does |
|---------|----------------|
| `afk-time-threshold-seconds` | Idle time before AFK starts |
| `payout-interval-seconds` | How often points are awarded |
| `points-per-interval` | Points per payout |
| `min-active-seconds-before-afk` | Must play actively this long before first AFK (anti-AFK-farm on join) |
| `max-afk-seconds-per-day` | Daily cap (`-1` = off) |
| `max-afk-seconds-per-session` | Per-login cap (`-1` = off) |
| `blocked-gamemodes` | e.g. `CREATIVE`, `SPECTATOR` |

**Notifications** (title, subtitle, sounds on AFK start/end) — edit under `afk.start-notification` and `afk.end-notification`. Particles: `afk.particles.enabled`.

---

## 🛒 Shop setup

Add items under `shop.items`. Each item needs a unique key (`vip`, `diamonds`, …), a `slot`, `material`, `price`, and `commands` to run on buy.

<p align="center">
  <img src="https://raw.githubusercontent.com/rmue11er/snoozeloot/main/docs/summary/shop-ingame.png" alt="Snooze Shop GUI" width="640">
</p>

```yml
shop:
  size: 27
  confirm-above-price: 50    # second click required at this price or above
  transaction-log-max-entries: 500
  items:
    vip:
      slot: 11
      material: DIAMOND
      name: "<aqua>VIP Rank</aqua>"
      lore:
        - "<gray>Cost: <gold><price></gold> points</gray>"
      price: 100
      purchase-limit: -1       # -1 = unlimited, 3 = max 3 buys ever
      commands:
        - "lp user %player% parent add vip"
```

**Command placeholders:** `%player%` `%uuid%` `%world%` `%points%`

Names and lore use **MiniMessage** (`<gold>`, `<gray>`, …). Put `<price>` in lore — SnoozeLoot fills it in.

---

## 🌍 Worlds & WorldGuard

**Limit AFK to specific worlds** — list world names. Empty list = all worlds.

```yml
afk:
  allowed-worlds:
    - world
    - afk_world
```

**Limit AFK to WorldGuard regions** — install WorldGuard, then list region **IDs** (the name you gave the region in WorldGuard). Empty list = AFK allowed everywhere.

```yml
afk:
  allowed-regions:
    - afk_zone
    - spawn_afk
```

Example: you create a region `/rg define afk_zone` in WorldGuard. Players only earn points while standing inside `afk_zone`. Outside = no AFK rewards.

Both filters can be combined. If WorldGuard is not installed, `allowed-regions` is ignored.

---

## ⚡ Permission multipliers

Give VIP players faster AFK earnings. Map a permission to a multiplier — highest permission wins.

```yml
multipliers:
  snoozeloot.multiplier.vip: 1.5
  snoozeloot.multiplier.mvp: 2.0
```

Grant with LuckPerms: `/lp user Steve permission set snoozeloot.multiplier.vip true`

---

## 💸 Pay between players

```yml
pay:
  enabled: true
  min-amount: 1
  cooldown-seconds: 30
```

Players with `snoozeloot.pay` use `/snooze pay <player> <amount>`. Set `enabled: false` to disable.

---

## 🎁 Bonuses & streaks

Daily and weekly bonuses are claimed automatically when a player enters AFK. Streaks boost AFK earnings if the player had enough active play time that day.

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

---

## 🛡️ Anti-exploit

```yml
anti-exploit:
  ignore-movement-when-in-water: true
  ignore-movement-when-in-vehicle: true
  ignore-movement-when-on-rails: true
  untrusted-movement-input-grace-seconds: 4

afk:
  pool-detection:
    enabled: true
    samples: 12
```

- **Water / vehicle / rails** — environmental movement does not count as player activity
- **Pool-farm detection** — ignores players stuck in circular AFK pool machines
- **Shop** — purchase limits, confirm click for expensive items, transaction log

`/snooze history` for players · all server purchases with `snoozeloot.history.admin`

---

## 📊 PlaceholderAPI

Install [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/). SnoozeLoot registers automatically — no extra setup.

| Placeholder | Output |
|-------------|--------|
| `%snoozeloot_points%` | Point balance |
| `%snoozeloot_rank%` | Leaderboard rank (`-` if unranked) |
| `%snoozeloot_afk%` | `true` / `false` |
| `%snoozeloot_streak%` | Streak days |

Works in TAB, scoreboards, chat plugins, etc.

---

## 📈 Leaderboard

`/snooze stats` shows the top players **and your own rank**, even if you are not in the top list.

<p align="center">
  <img src="https://raw.githubusercontent.com/rmue11er/snoozeloot/main/docs/summary/stats-ingame.png" alt="/snooze stats — leaderboard and your rank" width="640">
</p>

```yml
stats:
  top-size: 10    # how many players appear in the top list
```

---

## 💾 Storage & language

```yml
storage:
  type: yaml    # or sqlite
language: en    # messages_en.yml or messages_de.yml
```

- **yaml** → `players.yml`, `meta.yml`, `transactions.yml`
- **sqlite** → single `snoozeloot.db` (better for larger servers)

Edit messages in `messages_en.yml` / `messages_de.yml` (MiniMessage tags). `/snooze reload` applies config + message changes. New config keys are merged automatically on upgrade (`config-version`).

**Switching storage** (`yaml` ↔ `sqlite`): change `storage.type` on a fresh server or migrate data manually — there is no built-in converter.

---

## ⌨️ Commands

| Command | Permission | What it does |
|---------|------------|----------------|
| `/snooze` | `snoozeloot.use` | Your balance |
| `/snooze shop` | `snoozeloot.use` | Open shop |
| `/snooze stats` | `snoozeloot.use` | Leaderboard + your rank |
| `/snooze show <player>` | `snoozeloot.use` | Someone else's balance |
| `/snooze pay <player> <amount>` | `snoozeloot.pay` | Send points |
| `/snooze history [limit]` | `snoozeloot.use` | Your purchases |
| `/snooze history [limit]` | `snoozeloot.history.admin` | All server purchases |
| `/snooze help` | `snoozeloot.use` | Commands you can run |
| `/snooze admin` | `snoozeloot.admin` | Show AFK/payout settings |
| `/snooze give <player> <amount>` | `snoozeloot.admin` | Add points |
| `/snooze remove <player> <amount>` | `snoozeloot.admin` | Remove points |
| `/snooze set <player> <amount>` | `snoozeloot.admin` | Set exact balance |
| `/snooze reset <player>` | `snoozeloot.admin` | Set balance to 0 |
| `/snooze reload` | `snoozeloot.admin` | Reload config |
| `/snooze debug [player]` | `snoozeloot.admin` | AFK debug info |

---

## 📋 Requirements

- **Paper 1.20.5+** (Java 21+)
- PlaceholderAPI — optional
- WorldGuard — optional

Uses [bStats](https://bstats.org) (ID **32608**) for anonymous server stats. No player names or IPs. Opt out: `bstats.enabled: false` in `config.yml`, or disable bStats server-wide in `plugins/bStats/config.yml`.

[Source](https://github.com/rmue11er/snoozeloot) · [Issues](https://github.com/rmue11er/snoozeloot/issues)
