# Settings appearance

Settings appearance lets a user open Settings from the home gear, change language between English and Arabic, and change theme among System, Light, and Dark.

## Sub-features

- `settings-open` opens Settings from the home `Settings` icon.
- `settings-language-ar` switches language to `العربية`.
- `settings-theme-dark` switches theme to Dark (`داكن` once Arabic is on).
- `settings-back` returns to Husn with `Back`.

## How to get to it (user POV)

- Tap `Settings` on the Husn TopAppBar.
- Tap `Language` then `العربية` or `English`.
- Tap `Theme` then `Dark` / `Light` / `System` (Arabic labels `داكن`, `فاتح`, `النظام` after switching language).

## Driving it with control-azkarify

Preconditions:

- Azkarify is healthy on `AzkarifyVerify`.
- Language is English.

- **Open Settings.** Run `tap --desc "Settings"`. `wait --text "Language"` succeeds.
- **Arabic.** Run `tap --text "Language"` then `tap --text "العربية"`. `wait --text "المظهر"` succeeds.
- **Dark theme.** Run `tap --text "المظهر"` then `tap --text "داكن"`. `داكن` remains selected.
- **Return.** Run `tap --desc "Back"`. Home copy is Arabic (search description becomes the Arabic `Search azkar` string). Restore English before other English feature recipes: reopen Settings and choose `English`.
- **Proof.** Capture Settings after Arabic + dark. Run `dump --path "$AZKARIFY_VERIFY_EVIDENCE/settings-appearance/arabic-dark.xml"` and `screenshot --path "$AZKARIFY_VERIFY_EVIDENCE/settings-appearance/arabic-dark.png"`. Artifacts show `العربية` and `داكن`.

## Gotchas

- Accent color opens a separate picker; it is not required for this feature.
- Reminder rows show times such as `08:00`. Tapping them opens a Material time picker. Cancel with `Close`; save with `Save`. Do not leave a draft time picker open for other recipes.
- `Support the developer` may open RevenueCat. Do not purchase.
- Language change reloads categories. Wait for Settings copy or home titles; do not tap stale English nodes.
