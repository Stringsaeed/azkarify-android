---
name: verify-azkarify-android
description: Drive the Azkarify Android Compose app on emulator AzkarifyVerify — launch, doctor, tap real screens, capture evidence. Use when proving home search, reading/counter, favorites, journeys, or settings.
---

# Verify Azkarify Android

Scripted user-path proof for the native Kotlin / Jetpack Compose app `com.stringsaeed.azkarify` in this repo. Read this skill cold, then the matching file under [features/](features/README.md). Do not improvise selectors.

**Primary surface:** the Android app (Material TopAppBar, overflow `Menu`, inline `Search azkar` on Husn / Favorites). Users launch `MainActivity`; there is no web UI and no CLI.

**Noted, not driven here:** the iOS sibling at `../azkarify-ios` (`azkarify.xcodeproj`, XCUITest under `azkarifyUITests/`). iOS Simulator requires Xcode / `xcodebuild` / `xcrun simctl` on macOS. Linux cloud VMs do not provide those binaries; do not invent an iOS proof. The iOS repo also ships a local content editor (`python3 content-editor/server.py` on `http://127.0.0.1:8765`) that edits bundled JSON — that is authoring tooling, not the reader app.

**Existing harness (optional, not the agent drive path):** `./gradlew :app:connectedDebugAndroidTest` and `./gradlew :app:testDebugUnitTest`. Compose tests already encode the same labels. Prefer `control-azkarify` so an agent can stop at one mapped feature and keep screenshots.

## Launch

SDK: `ANDROID_HOME` (or `sdk.dir` in gitignored `local.properties`). Debug APK via `./gradlew :app:assembleDebug`. Application id `com.stringsaeed.azkarify`, launcher `com.stringsaeed.azkarify/.MainActivity`. Min SDK 26.

One disposable AVD only: **AzkarifyVerify** (Pixel 6 skin, `system-images;android-30;google_apis;x86_64`, port 5554 → serial `emulator-5554`). Do not attach to a physical phone or a differently named emulator.

```sh
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
export AZKARIFY_ANDROID_ROOT="$(git rev-parse --show-toplevel)"
# Optional: keep proof in-repo. Cleanup must not delete this directory.
export AZKARIFY_VERIFY_EVIDENCE="$AZKARIFY_ANDROID_ROOT/docs/verification/control-skill"
python3 .cursor/skills/verify-azkarify-android/scripts/control-azkarify launch
```

Ready when the helper prints `ready serial=emulator-5554` and home shows either intro `Skip` (dismissed automatically) or `Search azkar` plus a category titled `Words of remembrance for morning and evening`. First-run also shows a location sheet; launch taps `Not now`.

Teardown is `control-azkarify cleanup` (kills the recorded emulator PID / `adb -s <serial> emu kill` only). Never `pkill emulator`.

If AzkarifyVerify is already online, launch refuses unless you pass `--reuse` for the instance **this** run started. Two agents must not share the AVD: SharedPreferences (`azkarify`) and journey DataStore live on that virtual device.

## Doctor

Run this first whenever anything looks off:

```sh
python3 .cursor/skills/verify-azkarify-android/scripts/control-azkarify doctor
```

Require:

- `serial=emulator-5554` (or the serial launch printed) listed as `device`
- `avd=AzkarifyVerify`
- `boot_completed=1`
- `package_installed=True`
- `activity_in_dumpsys=True` after a successful start

If doctor fails, cleanup (if this run started the emulator) and relaunch. Do not tap another device.

## Drive

Harness: `control-azkarify` over `adb` + `uiautomator dump`. Prefer content descriptions and exact English copy from `data/UiStrings.kt` / `AppFlowTest.kt`. Compose test tags (`azkarCategoryList`, `azkarListPane`, `azkarEntriesList`, `azkarJourneyList`, `azkarDetailPane`) exist for Espresso; the dump path uses visible text and `content-desc`.

```sh
python3 .cursor/skills/verify-azkarify-android/scripts/control-azkarify dump --path "$AZKARIFY_VERIFY_EVIDENCE/window.xml"
python3 .cursor/skills/verify-azkarify-android/scripts/control-azkarify wait --desc "Search azkar"
python3 .cursor/skills/verify-azkarify-android/scripts/control-azkarify tap --desc "Search azkar"
python3 .cursor/skills/verify-azkarify-android/scripts/control-azkarify type --text "before sleeping"
python3 .cursor/skills/verify-azkarify-android/scripts/control-azkarify wait --text "What to say before sleeping"
python3 .cursor/skills/verify-azkarify-android/scripts/control-azkarify screenshot --path "$AZKARIFY_VERIFY_EVIDENCE/search-azkar/filtered.png"
```

Stable handles:

| Handle | Kind | Where |
| --- | --- | --- |
| `Skip` | text | intro pager |
| `Not now` / `Set location` | text | first-run prayer sheet |
| `Search azkar` | content-desc | Husn and Favorites list search |
| `Clear search` | content-desc | trailing search icon when query non-empty |
| `Settings` | content-desc | home TopAppBar |
| `Menu` | content-desc | home overflow |
| `Favorites`, `Counter`, `Quick Mode`, `Journeys`, `Prayer times` | text | overflow menu |
| `Add to favorites` / `Remove from favorites` | content-desc | category star |
| `Words of remembrance for morning and evening` | text | first category (id 27) |
| `What to say before sleeping` | text | category id 28 |
| `Count: N` | content-desc | counter number |
| `Reset` | text | counter |
| `Mark complete: <step title>` | content-desc | journey step |
| `Back` | content-desc | non-home app bars |

Type via `input text`; spaces become `%s`. Submit search with `key --code 66` (Enter) if the keyboard is up. Wait for the result node, not a fixed sleep.

Start every recipe from home after launch (intro skipped, location dismissed, English, search empty) unless the feature file says otherwise.

## Evidence

Default directory: `$AZKARIFY_VERIFY_EVIDENCE` if set, otherwise `.cursor/skills/verify-azkarify-android/artifacts/`. Cleanup never deletes it.

Proof standards:

- Exercise the real activity path (`am start` of `MainActivity`). Do not poke SharedPreferences or DataStore to fake the user action under test. Launch may tap `Skip` / `Not now` so home is reachable.
- Capture the action dump or screenshot and the resulting state (filtered list, open entry, points, persisted setting).
- Side effects: favorites survive opening `Menu` → `Favorites`; language/theme survive activity death only if you recreate; journey points are on-screen (` 35 ` in the detail pane for Heading out). Do not assert JVM unit tests as UI proof.
- RevenueCat / Play: Settings may open a paywall. Do not purchase or restore. Network to Play is a production boundary.
- City catalog on Android is incomplete (no App Attest contract). Prayer location uses manual coordinates or device GPS. Do not treat a missing country picker as a product regression versus iOS.

Record the feature id and entry point with every artifact (filename prefix or a sibling `notes.txt`).

## Cleanup

```sh
python3 .cursor/skills/verify-azkarify-android/scripts/control-azkarify cleanup
```

Stops the emulator recorded in `/tmp/azkarify-verify/state.json` (override with `AZKARIFY_VERIFY_STATE`). Does not delete evidence. Does not uninstall SDK or the AVD definition. After a failed iteration, run cleanup before the next launch so port 5554 is free.

Confirm evidence still exists at the named path after cleanup.

## Helpers

Executable helper (invocation is the commands above):

`.cursor/skills/verify-azkarify-android/scripts/control-azkarify`

Subcommands: `launch`, `doctor`, `dump`, `wait`, `tap`, `type`, `key`, `screenshot`, `cleanup`.

Repo screenshot helper for foldable multi-display captures (not required on AzkarifyVerify): `python3 scripts/capture_android_screenshot.py emulator-5558 output.png`.

## Maintenance

When screens or copy change, update this skill and the feature map with `/maintain-verification-skill`.
