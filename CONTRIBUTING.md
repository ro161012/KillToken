# Contributing to KillToken

Thanks for taking the time to contribute! The guidelines below keep the
project consistent and easy to maintain.

## Getting started

1. Fork the repository and create a topic branch from `main`
   (e.g. `feature/drop-modes` or `fix/cooldown-edge-case`).
2. Build with Java 21+ and Maven 3.9+:

   ```bash
   mvn clean verify
   ```

3. Make your changes, add or update tests where appropriate, and ensure
   `mvn verify` passes.

## Code style

- Follow the existing formatting: 4-space indentation, no tabs.
- Public classes and public methods carry Javadoc; keep comments in
  imperative mood and explain *why*, not *what*.
- Keep the plugin dependency-free (Paper API only).
- New user-facing behaviour should be configurable in `config.yml` with a
  sensible default.

## Commit messages

Use [Conventional Commits](https://www.conventionalcommits.org):

```
feat: add configurable drop sound
fix: pair cooldown not expiring across reloads
docs: clarify /killtoken set behaviour in README
```

## Pull requests

- One concern per pull request.
- Describe what changed and why; link related issues.
- Update `README.md` and `CHANGELOG.md` when user-facing behaviour changes.

## Reporting issues

Use the bug report or feature request templates. Include your KillToken
version, server software/version, and steps to reproduce.
