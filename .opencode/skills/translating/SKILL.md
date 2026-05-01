---
name: translating
description: Guide for contributing translations to the Double Doors plugin.
---

# Translating the Double Doors plugin

This guide explains how to contribute translations for the Double Doors plugin using Crowdin. It covers the translation workflow, best practices for consistency, and how to keep translations up to date with ongoing development.

## Translation workflow

When translating update the files in `src/main/resources/lang/`:
- `defaults.json` contains all message keys with English defaults. This is the source of truth for new/modified keys.
- `english/en_US.json` contains the actual English translations used in the plugin. This should be kept in sync with `defaults.json` and is the basis for other locales.
- Other locale files (e.g. `english/en_GB.json`, `french/fr_FR.json`) inherit from `en_US.json` until explicitly localized.

## Tips for consistent translations

- Use the same terminology across all messages (e.g. "door", "linked door", "partner door").
- Keep placeholders (e.g. `{player}`, `{door}`) intact and in the same order as the English source.
- When in doubt, refer to the English messages for context and tone.
- If a message is unclear, check the plugin code or ask for clarification before translating.
