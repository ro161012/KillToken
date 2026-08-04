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
- **Runtime currency customization** — change the token to *any* item in the
  game with a single command. No config editing, no restart.
- **Configurable** — drop amount, cooldown length, and all messages live in
  `config.yml`.
- **Zero dependencies** — no libraries to install, works out of the box.

## How it works

KillToken listens to player death events and checks whether the killer is
another player. If so, it spawns the configured currency item directly at the
death location. The item includes fixed metadata (name and lore) so the token
always looks the same.

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
| `/killtoken set` | Sets the item in your **main hand** as the new Kill Token currency. All future drops use it. | `killtoken.admin` | `op` |
| `/killtoken reload` | Reloads `config.yml`. | `killtoken.admin` | `op` |

Both subcommands are tab-completed.

## Permissions

| Permission | Description | Default |
|-----------|-------------|---------|
| `killtoken.admin` | Manage the currency item and reload configuration. | `op` |

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

The currency item is stored under `currency-item` and is seeded automatically
with a named Nether Star on first launch. After running `/killtoken set`, the
held item (including its name, lore, and enchantments) is persisted there.

Color codes use the `&` prefix (e.g. `&6`, `&c`, `&l`).

## Building from source

Requires **Java 21+** and **Maven 3.9+**.

```bash
git clone https://github.com/ro161012/KillToken.git
cd KillToken
mvn clean package
```

The runnable jar is produced at `target/KillToken-<version>.jar`. Unit tests
run automatically during the build.

## Project layout

```
.
├── pom.xml                                  Maven build configuration
├── src/
│   ├── main/
│   │   ├── java/dev/ro161012/killtoken/
│   │   │   ├── KillTokenPlugin.java         Main class, config + currency item
│   │   │   ├── KillListener.java            Death event -> token drop logic
│   │   │   ├── PairCooldown.java            Symmetric pair-cooldown tracker
│   │   │   └── KillTokenCommand.java        /killtoken set|reload
│   │   └── resources/
│   │       ├── plugin.yml                   Plugin metadata
│   │       └── config.yml                   Default configuration
│   └── test/
│       └── java/dev/ro161012/killtoken/
│           └── PairCooldownTest.java        Anti-farming cooldown tests
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

---

<div align="center">
  Made with care by <a href="https://github.com/ro161012">ro161012</a>.
</div>
