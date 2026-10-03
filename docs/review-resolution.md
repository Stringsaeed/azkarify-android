# Android review resolution

## Correctness

- Content caches use `ConcurrentHashMap.computeIfAbsent`, with atomic population
  per language/document. A concurrent test checks 160 real bilingual asset reads.
- Startup preference values are parsed by type, with defaults and validation for
  theme/accent/language. The corruption test supplies wrong preference types and
  verifies that the ViewModel initializes without crashing. Forced null assertions
  were removed.
- The alarm finding was incorrect. `setAndAllowWhileIdle` is an inexact API and
  does not require exact-alarm permission. The app does not declare that permission;
  an emulator test schedules and cancels its reminder successfully. Notification
  permission handling remains separate.

[Android alarm guidance](https://developer.android.com/develop/background-work/services/alarms)

## Rendering and storage

- Botanical paths, brushes, geometry, and stroke objects are built in `drawWithCache`.
  Drawing replays cached objects. Size, density, palette, direction, or variant changes
  invalidate the cache so adaptive and RTL rendering remain correct.
- Journey persistence uses Preferences DataStore. The old `journey-progress.v2`
  preference migrates without changing other preferences. Transactions serialize on
  `Dispatchers.IO`; UI observes committed snapshots. Audio feedback plays after a
  successful commit, with released audio objects guarded against later callbacks.
- Total/today points are cached when the snapshot is built. UI point reads no longer
  iterate over all historical awards. Concurrent updates are tested with 40 steps;
  every completion survives and the completion bonus is awarded once.
- DataStore still stores the existing JSON representation for compatibility. Full
  snapshot encoding runs off the UI thread. This is not an indexed database; very
  large histories may eventually justify a Room migration. No frame-rate or GC
  improvement is claimed without profiling.

## Localization, navigation, and build

- 215 UI labels are in `res/values/strings.xml` and `res/values-ar/strings.xml`.
  Asset copy JSON and inline bilingual routine/method labels were removed. Routine
  structure references resource-backed labels; devotional text remains unchanged
  in the bilingual content files.
- Android 13+ language selection uses `LocaleManager` and the manifest locale config.
  System locale changes refresh UI copy without rewriting the user's language on
  every rotation. Older versions use localized resource contexts for UI copy.
- Navigation uses serializable, typed `Destination` keys in a Navigation 3
  `NavBackStack`, saved with its closed-hierarchy serializer. Material Adaptive still
  controls pane rendering. Raw route strings and route parsing are gone.
- All direct external dependencies and Kotlin plugins use the version catalog.

## Verification

Regression tests cover concurrent content reads, migration of existing points,
concurrent progress updates, malformed preference types, reminder scheduling,
Arabic/dark preferences, topic search, counter restoration, adaptive pane selection,
and journey award persistence. Device XML reports are saved under
`docs/verification/review-tests/`.

Localization generation is idempotent, and all 267 source asset/font checksums match.
These checks are correctness evidence, not physical-device performance measurements.

Final checks: 30 instrumented checks pass across Pixel 10a, Pixel Tablet, and Pixel Fold; 11 unit tests pass. The tests use the current Compose test dispatcher, wait for rendered content, and scroll to virtualized controls before tapping.
