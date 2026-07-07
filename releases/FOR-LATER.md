# For Later

This file contains updates that we need to document in a later release when that comes.

## Changes

- `/dd locale credits` and `/dd locale credit <code>` for listing translation credits.
- Completed the locale command strings across bundled translations instead of leaving English fallbacks in non-English language files.
- Migrated the build and release pipeline from Maven to Gradle, including the Bukkit/Velocity module builds, the Gradle wrapper, and the GitHub Actions `publish` workflow.
- Added a GitHub Actions CI workflow that builds the project on pushes and pull requests.
