# KillToken

A Paper plugin that drops a **Kill Token** item whenever a player kills another
player. The token is a configurable currency item, and a pair-based cooldown
stops two players from farming tokens off each other.

Requires Paper 1.21+ and Java 21+.

## Features

- Tokens drop only when a player deals the killing damage. Mobs, fall damage,
  lava, drowning, void and suicide never drop tokens.
- Pair cooldown: after A kills B, neither A→B nor B→A drops a token for the
  configured duration. Kills against other players are unaffected.
- Killstreaks: consecutive kills show a counter on the action bar and play a
  sound that rises in pitch. Streaks reset on death or disconnect.
- Compressed Kill Token Block: a quartz block worth 64 tokens, with the value
  written in its lore. No crafting recipes are included; set up trades with
  your own villagers or shops.
- `/killtoken give` and `/killtoken giveblock` place items directly into a
  player's inventory. Overflow drops at their feet.
- The compressed block cannot be placed in the world.
- Drop amount, cooldown length, messages and the block material are
  configurable in `config.yml`.

## Commands

| Command | Description |
|---|---|
| `/killtoken set` | Use the item in your main hand as the Kill Token currency. |
| `/killtoken give [player] [amount]` | Give Kill Tokens (defaults to you, 1 token). |
| `/killtoken giveblock [player] [amount]` | Give compressed blocks, each worth 64 tokens. |
| `/killtoken reload` | Reload `config.yml`. |

All commands default to operators, with a separate permission per subcommand
(`killtoken.set`, `killtoken.give`, `killtoken.reload`, all children of
`killtoken.admin`).

## Configuration

The plugin creates `plugins/KillToken/config.yml` on first run.

```yaml
# Seconds between drops for the same pair of players.
cooldown-seconds: 60

# Tokens dropped per qualifying kill.
tokens-per-kill: 1

# Message sent to the killer when a token drops. Empty disables it.
kill-message: "&6+1 Kill Token"

killstreak:
  enabled: true
  message: "&6Killstreak&8: &f%streak%"   # %streak% = current streak
  sound: ENTITY_EXPERIENCE_ORB_PICKUP
  base-pitch: 0.7
  pitch-per-kill: 0.15
  max-pitch: 2.0

compressed-blocks:
  compressed-block-material: QUARTZ_BLOCK  # material of the 64-token block
```

The currency item itself is stored under `currency-item` and is replaced with
whatever you hold when running `/killtoken set`.

## Building

```sh
./mvnw clean package
```

Requires Java 21. The jar is written to `target/KillToken-<version>.jar`.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) and [SECURITY.md](SECURITY.md).

## License

[MIT](LICENSE)
