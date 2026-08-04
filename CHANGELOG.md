# Changelog

All notable changes to KillToken are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
  into an inventory. Amounts are validated against 1&ndash;2304.
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
