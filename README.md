# Azkarify for Android

Native Kotlin and Jetpack Compose port of `/Users/saeed/projects/azkarify`.
One app module, an Android ViewModel, an asset repository, and small platform services.
Typed Navigation 3 keys drive navigation, and Material Adaptive handles panes.
The azkar reading experience works offline. No account or devotional content
download is needed. RevenueCat handles optional support purchases.

The Android privacy policy is published at
[azkarify.saeed.sh/android/policy](https://azkarify.saeed.sh/android/policy).
Its static source is in `site/`. Settings links to the same URL.

## Run

Open this directory in Android Studio, or use:

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest
./gradlew :app:assembleRelease
```

Android 8.0 / API 26 or later. The release build enables R8 and resource shrinking.
To build a signed bundle, place the upload keystore at
`signing/azkarify-upload.jks`, copy `keystore.properties.example` to
`keystore.properties`, and set both passwords. Keep the keystore and passwords
in secure backups outside this repository. Both local files are ignored by Git.
Then run `./gradlew :app:bundleRelease`. The bundle is written to
`app/build/outputs/bundle/release/app-release.aab`. Without
`keystore.properties`, release builds remain unsigned. Google Play uses the
upload certificate to accept the bundle and its separate app-signing
certificate to sign installs. The Android application ID is
`sh.saeed.azkarify`; the iOS bundle ID remains
`com.stringsaeed.azkarify`. Android treats the former
`com.stringsaeed.azkarify` package as a separate installation, so its local
favorites, progress, and settings do not move to the new package automatically.

## Code

- `data/AzkarRepository.kt`: bundled bilingual content and per-document cache. Reads run on the IO dispatcher.
- `data/AppViewModel.kt`: preferences and screen state, exposed through StateFlow.
- `data/JourneyProgress.kt`: daily checklist, choices, and awards that cannot be earned twice for a step on the same day. `JourneyProgressRepository.kt` supplies atomic DataStore persistence on IO workers.
- `ui/`: Compose screens, shared appearance, RTL layout, and lazy scrolling lists.
- `services/`: Adhan prayer calculations, Android notification scheduling, and
  the authenticated country and city catalog.

Navigation and counter state survive activity recreation. Favorites, theme, accent,
language and prayer configuration persist locally. Journey records use DataStore
with automatic migration from the previous preference payload. Completed steps
award 10 points; completing all required steps awards 25 points. Reopening a step does
not remove or repeat its award. Optional steps do not affect journey completion.

## Country and city catalog

The city picker uses the 11 locations bundled from iOS when online access is
unavailable. It can also browse the full countries API, search cities in
English or Arabic, and load more pages. Downloaded pages are cached privately
on the device for 24 hours and revalidated with ETags.

Release builds use Play Integrity standard tokens bound to the complete request
URL, language, and ETag. The linked Google Cloud project number is
`97729473680`. Release currently uses the same staging API base URL as iOS.
The backend must allow `sh.saeed.azkarify` and the Play app-signing certificate,
and it must have service-account credentials for the linked Cloud project.
Verify this path with a signed build installed from a Play testing track on a
certified physical device. An emulator or sideloaded APK cannot prove the
required Play verdicts.

Debug builds use the local API's bearer token. Start the local countries API,
run `adb reverse tcp:8787 tcp:8787`, and build with
`AZKARIFY_CATALOG_DEV_TOKEN` set to the same token as the API's `DEV_AUTH_TOKEN`.
The token is embedded only in the debug build. Without it, debug builds keep
the bundled city picker and make no catalog requests.

## Material 3 Expressive and compatibility

The app uses `MaterialExpressiveTheme`, expressive motion, emphasized Alan Sans
text styles, morphing buttons, interactive Material list items, native cards,
loading indicators, a wavy counter indicator, and Material time-picker dialogs.
Headers, menus, search, switches, and sheets are Android Material components.
Inline list search uses the current TextFieldState/SearchBarState input API.
Brand colors, fonts, devotional artwork, and content remain shared with iOS.

Material 3 Expressive currently requires the pinned **1.5.0-alpha29** release.
The matching Compose alpha BOM avoids mixing incompatible Foundation/UI versions.
This is a prerelease dependency; upgrades should repeat the runtime checks.
Compile SDK 37.2 satisfies library metadata requirements. The minimum runtime
remains API 26 and target SDK remains 37. No Android 17 runtime is required.
Short windows use keyboard time input; tablets and unfolded devices use adaptive
list/detail panes. Arabic RTL, dark mode, and activity recreation are covered.
See [Expressive verification](docs/verification/expressive/README.md).

## Shared assets

266 JSON files are copied unchanged from iOS. Alan Sans includes Arabic glyphs.
Regular and Bold font files are instantiated at weights 400 and 700 from the same
variable source because Android otherwise uses its default Light weight.
The crescent, currency, and launcher artwork are rasterized from the original SVGs.
Colors, typography, spacing, and botanical paths are based on the SwiftUI source.
Cards and interactive controls use Material shapes and behavior. Navigation uses Android Material TopAppBar, an
overflow DropdownMenu, and a Material search input above searchable lists. These
platform controls follow Android layout and RTL conventions. The iOS header
pills and bottom floating search are deliberately excluded. Android also uses
its own emoji and system controls.

```sh
python3 scripts/import_ios_assets.py ../azkarify
python3 scripts/verify_ios_assets.py ../azkarify
python3 scripts/import_ios_cities.py ../azkarify --check
# Tooling only; needs fonttools and sharp. Outputs are already bundled.
python3 scripts/instantiate_fonts.py
node scripts/render_artwork.cjs
```

## Remaining parity work

- Live full-catalog verification waits for Play app signing and backend
  credentials. The backend setup chat is updating its former package ID.
- RevenueCat is configured with the existing Android public SDK key. Settings opens
  its current dashboard offering and native paywall. The AED 9.99 product was observed
  on the emulator. Store purchase and restoration
  need Play sandbox verification; no purchase is made during local checks.
- Adhan Kotlin supports 11 of the iOS app's 12 calculation methods. Tehran's separate
  Maghrib angle is not exposed by this library and is not offered in the picker.
- Native Android emoji, notification controls, audio cues, and material effects differ
  from Apple's platform assets. Pixel-identical cross-platform parity is unverified.
- Android reminders use inexact idle-aware alarms and may be delayed by the OS.
  Permission denial is handled, and alarms restore after reboot or time-zone changes.
- Tablet and foldable layouts are verified in emulators; see [adaptive verification](docs/verification/adaptive/README.md). Android 8/API 26 is also covered by runtime tests. Notification
  delivery after reboot and physical-device performance still need dedicated verification.

The iOS source is kept unchanged.

Build, runtime evidence, and limitations are recorded in [verification](docs/verification/verification.md).

The latest audit changes and their evidence are described in [review resolution](docs/review-resolution.md).
