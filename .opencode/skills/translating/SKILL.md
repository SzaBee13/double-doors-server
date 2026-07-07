---
name: translating
description: Guide for contributing and maintaining Double Doors translation files in core/src/main/resources/lang, including Crowdin workflow, key ordering, and locale consistency.
---

# Translating the Double Doors plugin

Use this skill when translating or reviewing Double Doors language files, especially when adding new message keys, updating existing text, or syncing Crowdin output back into the repo.

## Workflow

Work in `core/src/main/resources/lang/`:
- Update `defaults.json` first. It is the source of truth for message keys and English defaults.
- Update `en_US.json` second. Keep it aligned with `defaults.json`; this is the fallback for every locale.
- Update other locale files only when localization is requested or already maintained for that language.
- Keep key order stable unless a new key is being inserted alongside related entries.

## Translation rules

- Preserve placeholders exactly, including braces and order, for example `{player}`, `{door}`, `{count}`.
- Keep terminology consistent across the plugin: prefer the same words for "door", "partner door", "linked door", and "locale".
- Match the tone of the source message. Short operational messages should stay short.
- Do not rewrite unrelated strings while editing a file.
- If a message is unclear or context-sensitive, inspect the calling code before translating.

## Locale policy

- Default behavior: only `en_US.json` is updated.
- Other English variants and regional locales inherit from `en_US.json` until explicitly localized.
- If the user asks for broader localization, update the requested locales together so they stay consistent.

## Crowdin notes

- Keep repository files and Crowdin output in sync.
- When importing translated text, verify that keys were neither added nor removed unexpectedly.
- Review translated punctuation, quotes, and line breaks before committing.

## See also

- `TRANSLATING.md`
