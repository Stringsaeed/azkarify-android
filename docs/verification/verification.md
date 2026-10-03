# Android verification, 3 October 2026

Native Kotlin / Compose application, `com.stringsaeed.azkarify`.
Test device: Pixel 10a emulator, Android 17 / API 37. iOS reference: iPhone 17 Pro,
iOS 27, built from the current native source without modifying it.

- Debug and optimized release builds pass. Release uses R8 and resource shrinking.
- 11 JVM tests pass: five journey state/points tests and six Adhan schedule tests.
- Three emulator tests pass: favorites and reading with counter reset/restoration;
  Arabic/dark preferences across recreation; journey progress and points across
  recreation, including reopening without duplicating awards.
- All 266 bundled JSON documents and the original font match the iOS source hashes.
  Both languages contain 132 categories, with valid referenced content.
- The configured RevenueCat offering opens its native dashboard paywall, showing
  the AED 9.99 support product and restore control. No purchase or restoration was
  performed. Screenshot: `android-support.png`.
- The initial release APK with RevenueCat was approximately 4.3 MB unsigned; the
  current adaptive build is approximately 4.5 MB.
- One cold activity launch of the initial release build reported 519 ms using `am start -W`.
  This is a single emulator measurement, not an app-startup benchmark.
- Mobile Dev recording `58724caa-82bf-4f2e-9451-4cba706840ac` failed because the
  Android CPU collector exited with code 139. The app stayed running. CPU, memory,
  FPS, and jank were not successfully captured, so no performance guarantee is made.

## Limits

The Android city-catalog authentication contract is not configured. Manual coordinates
and device location work without that catalog, with an explicit IANA time zone.
The Adhan Kotlin library does not expose the Tehran Maghrib angle supported by iOS.
Android emoji and system/material controls differ from Apple's versions. Physical
Android devices, older OS versions, and notification delivery after
reboot need additional verification. Signing for Google Play is not configured.

The release installed on the emulator uses the local debug signing key for testing.
The production release output remains unsigned.

## Android-native controls update

The navigation header now uses Material TopAppBar and the standard Android overflow
DropdownMenu, with automatic RTL placement. Search uses Material SearchBar above the
home and favorites lists, with live filtering, clear, and keyboard Search handling.
The custom iOS-style toolbar pills and bottom floating search were removed.
All four emulator UI checks pass, including query filtering and clearing. The earlier
home comparison is historical; `android-material-home.png` shows the current design.

## Tablet and foldable update

Android Material Adaptive now handles list/detail panes in medium and expanded
windows, compact single-pane navigation, and fold posture. All 20 UI test runs
pass across phone, tablet, unfolded foldable, and tabletop configurations. The 11 unit checks and release
build also pass. See [adaptive verification](adaptive/README.md) for current evidence.
