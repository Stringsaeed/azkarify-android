# Azkarify for Android

Native Kotlin and Jetpack Compose port of `/Users/saeed/projects/azkarify`.
One app module, an Android ViewModel, an asset repository, and small platform services.
Typed Navigation 3 keys drive navigation, and Material Adaptive handles panes.
The reading experience works offline. No account or content download is needed. RevenueCat handles support purchases when opened from Settings.

## Run

Open this directory in Android Studio, or use:

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest
./gradlew :app:assembleRelease
```

Android 8.0 / API 26 or later. The release build enables R8 and resource shrinking.
Release signing and Play Store setup have not been configured. The application ID
is `com.stringsaeed.azkarify`, matching the existing iOS app identity.

## Code

- `data/AzkarRepository.kt`: bundled bilingual content and per-document cache. Reads run on the IO dispatcher.
- `data/AppViewModel.kt`: preferences and screen state, exposed through StateFlow.
- `data/JourneyProgress.kt`: daily checklist, choices, and awards that cannot be earned twice for a step on the same day. `JourneyProgressRepository.kt` supplies atomic DataStore persistence on IO workers.
- `ui/`: Compose screens, shared appearance, RTL layout, and lazy scrolling lists.
- `services/`: Adhan prayer calculations and Android notification scheduling.

Navigation and counter state survive activity recreation. Favorites, theme, accent,
language and prayer configuration persist locally. Journey records use DataStore
with automatic migration from the previous preference payload. Completed steps
award 10 points; completing all required steps awards 25 points. Reopening a step does
not remove or repeat its award. Optional steps do not affect journey completion.

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
# Tooling only; needs fonttools and sharp. Outputs are already bundled.
python3 scripts/instantiate_fonts.py
node scripts/render_artwork.cjs
```

## Remaining parity work

- The iOS Countries API requires Apple App Attest. Its country/city picker needs an
  Android authentication contract on the backend. Android currently supports manual
  coordinates and device location, with an explicit IANA time zone.
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
