<div align="center">

# KillToken

**PvP kill currency for [Paper](https://papermc.io) Minecraft servers.**

A lightweight, dependency-free plugin that drops a recognizable **Kill Token**
item every time one player kills another — with built-in anti-farming
protection and a fully customizable currency item.

[![Build](https://img.shields.io/github/actions/workflow/status/ro161012/KillToken/build.yml?branch=main&logo=github&label=build)](https://github.com/ro161012/KillToken/actions)
[![Release](https://img.shields.io/github/v/release/ro161012/KillToken?logo=github&label=release)](https://github.com/ro161012/KillToken/releases)
[![Paper](https://img.shields.io/badge/Paper-1.21%2B-00A8A8?logo=markdown&logoColor=white)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-21%2B-orange?logo=openjdk&logoColor=white)](https://adoptium.net)
[![License](https://img.shields.io/github/license/ro161012/KillToken?label=license)](LICENSE)

[Features](#features) · [Installation](#installation) · [Commands](#commands) · [Configuration](#configuration) · [Building](#building-from-source)

</div>

---

## Overview

KillToken adds a simple but controlled PvP-based currency system. Whenever one
player kills another, a custom item — the **Kill Token** — is dropped at the
victim's death location. By default this is a **Nether Star** with a custom
name and lore, so it is clearly recognizable as a special currency item rather
than a normal drop.

Because the token is a real inventory item, it plugs naturally into shops,
trades, crates, or any other economy you build on top of it.

## Features

- **Player-kill driven drops** — a token is spawned at the victim's death
  location only when the killer is another player. Mobs, the environment, and
  self-kills never award tokens.
- **Consistent appearance** — every token carries fixed metadata (display name
  and lore), so it always looks the same and is easy to identify.
- **Anti-farming cooldowns** — a pair-based cooldown stops two players from
  trading kills to print tokens (see below).
- **Killstreaks** — consecutive PvP kills build a streak, announced on the
  action bar (the text above the hotbar, between health and hunger) with a
  sound whose pitch rises the higher the streak goes. Streaks reset on death
  or disconnect.
- **Always physical drops** — tokens always spawn on the floor, whether from
  a kill or from `/killtoken give`; they are never placed directly into an
  inventory.
- **Runtime currency customization** — change the token to *any* item in the
  game with a single command. No config editing, no restart.
- **Compressed Kill Token Block** — 64 tokens pack into a single
  quartz-textured storage block for trading (e.g. custom villager trades).
  No crafting recipes; you wire it into your own economy.
- **Configurable** — drop amount, cooldown length, and all messages live in
  `config.yml`.
- **Zero dependencies** — no libraries to install, works out of the box.

## How it works

KillToken listens to player death events and checks whether the killer is
another player. If so, it spawns the configured currency item directly at the
death location. The item includes fixed metadata (name and lore) so the token
always looks the same.

### What counts as a kill

A token drops **only when another player is responsible for the killing
damage**:

| Death cause | Token drops? |
|---|---|
| Melee kill by a player | Yes |
| Arrow / trident / projectile shot by a player | Yes (shooter is resolved as the killer) |
| Killed by a mob (zombie, creeper, …) | No |
| Fall damage, lava, drowning, void, fire, explosions | No |
| Suicide (`/kill`) | No |

In every case where a token drops, it spawns as a physical item entity on the
floor at the death location — never directly in an inventory.

The check is based on Paper's `Player#getKiller()`, which returns the player
who dealt the killing damage — or `null` for every non-player cause — plus an
explicit guard so players can never earn tokens from their own deaths.

### Anti-farming system

To prevent abuse, KillToken uses a **pair-based cooldown**:

- When **Player A** kills **Player B**, a cooldown (default: **60 seconds**) is
  applied to *that exact pair*.
- While the cooldown is active:
  - **A** killing **B** again drops **no token**, and
  - **B** killing **A** also drops **no token**.
- The cooldown is scoped to the pair only — kills involving anyone else are
  unaffected. For example, **A** killing **Player C** still drops a token
  normally.

This shuts down mutual kill-farming without penalizing legitimate PvP.

### Compressed Kill Token Block

64 Kill Tokens pack into a single **Compressed Kill Token Block** — a
quartz-textured block so it reads as "token-like" at a glance. There are no
crafting recipes; the block is meant to be wired into your own trading
(e.g. custom villager trades on an FFA server). Its lore states the token
value:

> **Compressed Kill Token Block**
> A compact block of Kill Tokens.
> **Value: 64 Kill Tokens**

The block material is configurable (`compressed-blocks.compressed-block-material`).

## Installation

1. Download the latest `KillToken-<version>.jar` from
   [Releases](https://github.com/ro161012/KillToken/releases).
2. Drop it into your server's `plugins/` folder.
3. Restart the server (or use a plugin manager that supports hot-loading).
4. A default `plugins/KillToken/config.yml` is created on first run.

**Requirements:** Paper (or a Paper fork such as Purpur/Folia*) 1.21+, Java 21+.

\* Folia is untested; the plugin uses only thread-safe primitives.

## Commands

| Command | Description | Permission | Default |
|---------|-------------|-----------|---------|
| `/killtoken set` | Sets the item in your **main hand** as the new Kill Token currency. All future drops use it. | `killtoken.set` | `op` |
| `/killtoken give [player] [amount]` | Spawns tokens on the floor at a player's feet (defaults to yourself, 1 token). | `killtoken.give` | `op` |
| `/killtoken giveblock [player] [amount]` | Spawns compressed Kill Token blocks (each worth 64 tokens; defaults to yourself, 1 block). | `killtoken.give` | `op` |
| `/killtoken reload` | Reloads `config.yml`. | `killtoken.reload` | `op` |

All subcommands are tab-completed (including online player names and amounts
for `give`/`giveblock`).

## Permissions

| Permission | Description | Default |
|-----------|-------------|---------|
| `killtoken.admin` | Umbrella permission granting every KillToken permission below. | `op` |
| `killtoken.set` | Change the Kill Token currency item. | `op` |
| `killtoken.give` | Hand out Kill Tokens directly. | `op` |
| `killtoken.reload` | Reload the configuration. | `op` |

## Configuration

`plugins/KillToken/config.yml`:

```yaml
# Seconds a killer<->victim pair must wait before another token drops.
cooldown-seconds: 60

# Number of token items dropped per qualifying kill.
tokens-per-kill: 1

# Message the killer when a drop is suppressed by the pair cooldown.
notify-on-cooldown: true
cooldown-message: "&cNo Kill Token dropped - you and this player are on cooldown."

# Message sent to the killer when a token drops. Leave empty to disable.
kill-message: "&6+1 Kill Token"
```

### Compressed Kill Token Block

```yaml
compressed-blocks:
  compressed-block-material: QUARTZ_BLOCK        # 64 tokens
```

The currency item is stored under `currency-item` and is seeded automatically
with a named Nether Star on first launch. After running `/killtoken set`, the
held item (including its name, lore, and enchantments) is persisted there.

Color codes use the `&` prefix (e.g. `&6`, `&c`, `&l`).

### Killstreaks

```yaml
killstreak:
  enabled: true
  message: "&6Killstreak&8: &f%streak%"   # %streak% = current streak length
  sound: ENTITY_EXPERIENCE_ORB_PICKUP      # any org.bukkit.Sound name
  base-pitch: 0.7                          # pitch at a streak of 1
  pitch-per-kill: 0.15                     # rise per consecutive kill
  max-pitch: 2.0                           # pitch cap
```

Every PvP kill shows the counter on the action bar for about a second and
plays the configured sound; the pitch climbs with each consecutive kill so a
rampage literally sounds different from a single kill. A streak ends when its
owner dies (any cause) or leaves the server.

## Building from source

Requires **Java 21+**. Maven is provided by the wrapper, so nothing else needs
to be installed.

```bash
git clone https://github.com/ro161012/KillToken.git
cd KillToken
./mvnw clean package        # mvnw.cmd on Windows
```

The build compiles the plugin, runs the full test suite (unit tests plus
MockBukkit integration tests for the plugin lifecycle, commands and kill
flow), and enforces the project's Checkstyle rules. The runnable jar is
produced at `target/KillToken-<version>.jar`.

## Project layout

```
.
├── pom.xml                                  Maven build configuration
├── mvnw / .mvn/                             Maven wrapper (reproducible builds)
├── config/checkstyle.xml                    Code style rules enforced in CI
├── src/
│   ├── main/
│   │   ├── java/dev/ro161012/killtoken/
│   │   │   ├── KillTokenPlugin.java         Main class, config + currency item
│   │   │   ├── KillListener.java            Death event -> token drop logic
│   │   │   ├── PairCooldown.java            Symmetric pair-cooldown tracker
│   │   │   ├── KillstreakTracker.java       Streak counter, action bar, pitch
│   │   │   ├── CompressedBlockManager.java  Compressed block items + recipes
│   │   │   └── KillTokenCommand.java        /killtoken set|give|giveblock|reload
│   │   └── resources/
│   │       ├── plugin.yml                   Plugin metadata
│   │       └── config.yml                   Default configuration
│   └── test/
│       └── java/dev/ro161012/killtoken/
│           ├── PairCooldownTest.java        Cooldown unit tests
│           ├── KillTokenPluginTest.java     Lifecycle & config integration tests
│           ├── KillTokenCommandTest.java    Command integration tests
│           ├── KillListenerTest.java        Kill-flow integration tests
│           └── KillstreakTrackerTest.java   Killstreak & pitch tests
└── .github/workflows/build.yml              CI build & test
```

## Roadmap / ideas

- [ ] Per-player killstreak bonuses
- [ ] Optional drop-on-ground vs. direct-to-inventory mode
- [ ] PlaceholderAPI / Vault hooks
- [ ] Sound & particle effects on drop

See [open issues](https://github.com/ro161012/KillToken/issues) or open your own.

## Contributing

Contributions are welcome — please read [CONTRIBUTING.md](CONTRIBUTING.md) and
follow the [code of conduct](CODE_OF_CONDUCT.md). For security issues, see
[SECURITY.md](SECURITY.md).

## License

This project is licensed under the [MIT License](LICENSE).
