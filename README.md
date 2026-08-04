<p align="center">
  <img src="docs/banner.png" alt="KillToken" width="100%" />
</p>

<h1 align="center">KillToken</h1>

<p align="center">
  A PvP kill currency for <a href="https://papermc.io">Paper</a> servers.<br/>
  Every player kill drops a Kill Token — with built-in anti-farming protection.
</p>

<p align="center">
  <a href="https://github.com/ro161012/KillToken/actions">
    <img src="https://img.shields.io/github/actions/workflow/status/ro161012/KillToken/build.yml?branch=main&logo=github&label=build" alt="Build status" />
  </a>
  <a href="https://github.com/ro161012/KillToken/releases">
    <img src="https://img.shields.io/github/v/release/ro161012/KillToken?logo=github&label=release" alt="Latest release" />
  </a>
  <a href="https://papermc.io">
    <img src="https://img.shields.io/badge/Paper-1.21%2B-00A8A8?logo=markdown&logoColor=white" alt="Paper 1.21+" />
  </a>
  <a href="https://adoptium.net">
    <img src="https://img.shields.io/badge/Java-21%2B-orange?logo=openjdk&logoColor=white" alt="Java 21+" />
  </a>
  <a href="LICENSE">
    <img src="https://img.shields.io/github/license/ro161012/KillToken?label=license" alt="License" />
  </a>
</p>

<p align="center">
  <a href="#features">Features</a> ·
  <a href="#installation">Installation</a> ·
  <a href="#commands">Commands</a> ·
  <a href="#configuration">Configuration</a> ·
  <a href="#building">Building</a> ·
  <a href="#faq">FAQ</a>
</p>

---

## ✨ Features

| ⚔️ **Player-kill drops** | 🛡️ **Anti-farming cooldown** | 📈 **Killstreaks** |
|---|---|---|
| Tokens drop only when another player deals the killing damage — melee, arrows, tridents. Mobs, fall damage, lava, drowning, void and suicide never drop. | After A kills B, the A↔B pair goes on cooldown (default 60s). While active, neither direction drops a token; kills against anyone else are unaffected. | Consecutive kills build a streak shown on the action bar, with a sound that rises in pitch. Streaks reset on death or disconnect. |

| 🪙 **Customizable currency** | 📦 **Compressed Kill Token Block** | 🎒 **Give-to-inventory** |
|---|---|---|
| The token is any item you choose — run `/killtoken set` holding it and every future drop uses it. No config editing, no restart. | A quartz block worth **64 tokens**, with its value written in the lore. No crafting recipes included; wire it into villager trades or shops. Can't be placed in the world. | `/killtoken give` and `/killtoken giveblock` place items directly into a player's inventory, merging with existing stacks. Overflow drops at their feet. |

| ⚙️ **Configurable** | 🧹 **Zero dependencies** | 🚀 **Lightweight** |
|---|---|---|
| Drop amount, cooldown length, messages, sounds and the block material all live in `config.yml`. | No libraries to install — drop the jar in and go. | Config and item templates are cached; kill events never re-parse config, sound names or color codes. |

## 📥 Installation

1. Download the latest `KillToken-<version>.jar` from the [Releases](https://github.com/ro161012/KillToken/releases) page.
2. Place it in your server's `plugins/` folder.
3. Restart the server.

A default `plugins/KillToken/config.yml` is created on first run.

**Requirements:** Paper (or a Paper fork) 1.21+ and Java 21+.

## 🎮 Commands

| Command | Description | Permission |
|---|---|---|
| `/killtoken set` | Use the item in your main hand as the Kill Token currency. | `killtoken.set` |
| `/killtoken give [player] [amount]` | Give Kill Tokens (defaults to you, 1 token). | `killtoken.give` |
| `/killtoken giveblock [player] [amount]` | Give compressed blocks, each worth 64 tokens. | `killtoken.give` |
| `/killtoken reload` | Reload `config.yml`. | `killtoken.reload` |

All commands are tab-completed, including player names and amounts.

### 🔐 Permissions

| Permission | Description | Default |
|---|---|---|
| `killtoken.admin` | Umbrella permission granting every permission below. | `op` |
| `killtoken.set` | Change the Kill Token currency item. | `op` |
| `killtoken.give` | Hand out Kill Tokens and compressed blocks. | `op` |
| `killtoken.reload` | Reload the configuration. | `op` |

## ⚙️ Configuration

```yaml
# Seconds a killer<->victim pair must wait before another token drops.
cooldown-seconds: 60

# Tokens dropped per qualifying kill.
tokens-per-kill: 1

# Message sent to the killer when a token drops. Leave empty to disable.
kill-message: "&6+1 Kill Token"

# Message when a drop is suppressed by the pair cooldown.
notify-on-cooldown: true
cooldown-message: "&cNo Kill Token dropped - you and this player are on cooldown."

killstreak:
  enabled: true
  message: "&6Killstreak&8: &f%streak%"   # %streak% = current streak length
  sound: ENTITY_EXPERIENCE_ORB_PICKUP     # any org.bukkit.Sound name
  base-pitch: 0.7                         # pitch at a streak of 1
  pitch-per-kill: 0.15                    # rise per consecutive kill
  max-pitch: 2.0                          # pitch cap

compressed-blocks:
  compressed-block-material: QUARTZ_BLOCK # material of the 64-token block
```

The currency item itself is stored under `currency-item` and is replaced
with whatever you hold when running `/killtoken set`.

Color codes use the `&` prefix (e.g. `&6`, `&c`, `&l`).

## 🛠️ Building

Requires **Java 21+**. Maven is provided by the wrapper.

```sh
git clone https://github.com/ro161012/KillToken.git
cd KillToken
./mvnw clean package        # mvnw.cmd on Windows
```

The build compiles the plugin, runs the test suite (45+ MockBukkit
integration and unit tests) and enforces Checkstyle. The jar is written to
`target/KillToken-<version>.jar`.

## ❓ FAQ

**How do I prevent players from farming tokens?**
The pair cooldown is on by default. Two players can't generate tokens from
killing each other repeatedly, and a single player can't earn from their own
death.

**Can I change what the token looks like?**
Yes — hold any item and run `/killtoken set`. The item (including its name
and lore) is persisted as the new currency.

**Why does my compressed block have an enchanted glint?**
It carries a hidden enchantment so it reads as a special currency item. The
enchantment is hidden, so the tooltip stays clean.

**Can players place the compressed block?**
No. Placement is cancelled — the block exists for trading, not building.

## 📄 License

This project is licensed under the [MIT License](LICENSE).

---

<p align="center">
  <sub>Built with ❤️ for the Minecraft community · [Report a bug](https://github.com/ro161012/KillToken/issues) · [Request a feature](https://github.com/ro161012/KillToken/issues)</sub>
</p>
