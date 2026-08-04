# Changelog

All notable changes to KillToken are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.3] - 2026-08-04

### Changed

- The default Kill Token lore now reads "Awarded for killing another player."
  The stored stock currency configuration with the old "slaying" wording is
  migrated automatically on startup or `/killtoken reload`; custom currency
  items are not changed.

## [1.2.2] - 2026-08-04

### Performance

- All configuration-derived values (drop amount, cooldown length, messages,
  killstreak settings and sound) are now resolved once when the config is
  loaded or reloaded and cached; death events and commands no longer re-parse
  YAML, enum names or color codes.
- The Compressed Kill Token Block item template is built once and cloned per
  use instead of being rebuilt with new item metadata on every call.
- The placement-rejection message is colorised once at class load.

No behavioral changes; this release is purely internal optimization.

## [1.2.1] - 2026-08-04

### Changed

- The Compressed Kill Token Block now has an **enchanted glint** (a hidden
  enchantment, so the tooltip stays plain).
- `/killtoken give` and `/killtoken giveblock` now place the items **directly
  into the target's inventory**, merging with existing stacks; overflow that
  does not fit is dropped at the player's feet instead of being lost.
- The Compressed Kill Token Block can **no longer be placed in the world**;
  placement is cancelled with a message. The block is for trading, not
  building. Kill drops still spawn on the floor as before.

## [1.2.0] - 2026-08-04

### Added

- **Compressed Kill Token Block**: packs **64 Kill Tokens** into a single
  quartz-textured storage block whose lore states its token value. No
  crafting recipes are provided - servers wire the block into their own
  trading (e.g. custom villager trades). The material is configurable via
  `compressed-blocks.compressed-block-material`.
- `/killtoken giveblock [player] [amount]` for handing out compressed
  blocks, using the `killtoken.give` permission. Blocks always spawn as
  physical drops, like tokens.

### Changed

- The default Kill Token lore is now a single plain line ("Awarded for
  killing another player.") - the "rare currency" flavor text was removed.

## [1.1.0] - 2026-08-03

### Added

- **Killstreaks**: consecutive PvP kills build a streak that is announced on
  the action bar (above the hotbar, between the health and hunger indicators)
  for about a second, with a sound whose pitch rises with every consecutive
  kill. Fully configurable (`killstreak.*`): enable flag, message, sound,
  base pitch, per-kill pitch step and pitch cap. Streaks reset on death from
  any cause and on disconnect.
- `/killtoken give [player] [amount]` for handing out tokens, e.g. for
  rewards or manual payouts (`killtoken.give` permission). Tokens always
  spawn as physical drops on the floor at the target's feet - never directly
  into an inventory. Amounts are validated against 1-2304.
- Fine-grained permissions: `killtoken.set`, `killtoken.give` and
  `killtoken.reload`, all children of `killtoken.admin`.
- MockBukkit-based integration test suite covering the plugin lifecycle,
  configuration seeding, every command path, permissions, the kill flow, the
  pair-based anti-farming cooldown, killstreak counting and the progressive
  sound pitch (35 tests total).
- Checkstyle enforcement in the Maven build (`config/checkstyle.xml`).
- Maven wrapper (`./mvnw`) for reproducible builds.

### Changed

- Extracted the PvP-only kill check into an explicit, documented
  `isPlayerKill` guard and documented exactly which death causes drop a token
  (melee and projectile kills by players qualify; mobs, environment, and
  suicide never do). Behaviour is unchanged.
- Split `reload()` into `reloadConfig()` + `applyConfig()` so runtime state
  synchronisation is independently testable.

## [1.0.0] - 2026-08-03

### Added

- Initial release.
- Drop a customizable **Kill Token** item at the victim's death location on
  player-versus-player kills (default: named & lored Nether Star).
- Pair-based anti-farming cooldown (default 60 seconds) blocking token drops
  between the same two players in either direction.
- `/killtoken set` — replace the currency item with the item in your main hand
  (persisted to `config.yml`).
- `/killtoken reload` — reload the configuration at runtime.
- Configurable drop amount (`tokens-per-kill`), cooldown length, and all
  player-facing messages.
- Unit tests for the pair-cooldown logic and a GitHub Actions CI build.
