# Azkarify Android verification map

This directory is the maintained source for verifying the user-facing behavior of Azkarify on Android. Read the index before driving the app, then use the matching feature file as the recipe.

## Baseline preconditions

- Launch Azkarify on AVD `AzkarifyVerify` with `control-azkarify launch`.
- Serial is the value launch printed (normally `emulator-5554`).
- Language is English. Intro is skipped. Location sheet is dismissed with `Not now`.
- Search is empty. Home list includes `Words of remembrance for morning and evening`.
- Put the helper on your command line from the Android repo root as `python3 .cursor/skills/verify-azkarify-android/scripts/control-azkarify`.
- Run `control-azkarify doctor` and require `avd=AzkarifyVerify`, `boot_completed=1`, and `package_installed=True`.
- Never drive an emulator this run did not start (or `--reuse` only for that same instance).

## Driving conventions

- Start every recipe from the baseline state unless its preconditions say otherwise.
- Prefer content descriptions and exact on-screen English copy over bounds or tab order.
- Treat every command as literal. Keep quoted names unchanged.
- Run UI actions through `control-azkarify`.
- Do not write SharedPreferences to skip a step that the feature file asks you to tap.
- Restore search-empty home after a mutation. Do not remove proof artifacts during cleanup.

## Proof and skip reporting

- Capture the user action and the resulting state, not only the final screen.
- UI proof includes a window dump and a screenshot with Azkarify identity visible (`Husn` or the topic title).
- Mutation proof includes a second user-facing view (Favorites list, reopened category, Settings after recreation).
- Record the feature ID and entry point used with every artifact.
- Report an unreachable path with the attempted command and the unmet precondition.
- Do not report a skipped entry point as verified through a different path.

## Feature entry contract

Each feature file starts with an H1 title and one paragraph describing the user-visible behavior. It then uses exactly four H2 sections in this order.

1. `Sub-features` lists short IDs with one line for each behavior.
2. `How to get to it (user POV)` lists every user entry point.
3. `Driving it with control-azkarify` starts with `Preconditions:` and uses labeled bullets that pair each user action with an exact command and observable result.
4. `Gotchas` lists traps that can waste or invalidate a verification run.

Keep implementation details out of the map. Name only user paths, stable handles, required state, commands, and observable proof.

## Features

- [Search azkar](./search-azkar.md) covers focusing search, filtering topics, empty results, and clearing.
- [Read and count](./read-and-count.md) covers opening a topic, countdown from repetitions, reset, and the standalone counter.
- [Favorites](./favorites.md) covers starring a topic and finding it only under Favorites.
- [Daily journeys](./daily-journeys.md) covers opening Heading out, completing the required step, and seeing points.
- [Settings appearance](./settings-appearance.md) covers language and theme choices from Settings.
