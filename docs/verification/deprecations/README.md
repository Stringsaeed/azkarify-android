# Material API deprecations

List search now uses SearchBarDefaults.InputField with TextFieldState and
SearchBarState. Its native input is hosted directly in a Material Surface for
inline list filtering. The current collapsed SearchBar disables the software
keyboard and expects an expanded search view, which does not fit this inline flow.
Saved queries and externally cleared text synchronize with the field state.

The modal sheet now uses rememberBottomSheetState with Hidden and Expanded
enabled, preserving the previous skip-partially-expanded behavior. The current
source also uses currentWindowAdaptiveInfoV2 and the auto-mirrored book icon.

A full non-incremental compileDebugKotlin completed with no app Kotlin warnings
or deprecation warnings. Debug APK packaging and lint passed. All 48 device checks
passed, 12 each on API 26 and the API 37 phone, tablet, and Fold. Reports are saved
here. Search focus, typing, filtering, clearing, and query restoration are covered.

The Android test runner still prints a Protobuf sun.misc.Unsafe deprecation
warning on the host JDK. This comes from its tooling dependency, not app source.
