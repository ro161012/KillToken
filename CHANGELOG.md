# Changelog

All notable changes to KillToken are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- `/killtoken give [player] [amount]` for handing out tokens directly, e.g.
  for rewards or manual payouts (`killtoken.give` permission). Items that do
  not fit in the target's inventory are dropped at their feet instead of
  being deleted. Amounts are validated against 1&ndash;2304.
- Fine-grained permissions: `killtoken.set`, `killtoken.give` and
  `killtoken.reload`, all children of `killtoken.admin`.
- MockBukkit-based integration test suite (26 tests) covering the plugin
  lifecycle, configuration seeding, every command path, permissions, the kill
  flow and the pair-based anti-farming cooldown.
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
