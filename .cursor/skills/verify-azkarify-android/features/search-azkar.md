# Search azkar

Search azkar lets a user filter the Husn topic list from the Material search field, see a matching topic, see no results for a miss, and restore the full list by clearing.

## Sub-features

- `search-focus` focuses the field only after a tap, not on cold start.
- `search-match` keeps matching titles and hides non-matches.
- `search-empty` shows `No results` for a query with no topics.
- `search-clear` removes the query and restores unfiltered topics.
- `search-submit` dismisses the keyboard without changing the filtered list.

## How to get to it (user POV)

- Tap the `Search azkar` field on Husn (home).
- Tap the same field on Favorites after `Menu` → `Favorites`.
- Type after the field is focused. Clear with the trailing `Clear search` icon when the query is non-empty.

## Driving it with control-azkarify

Preconditions:

- Azkarify is healthy on `AzkarifyVerify`.
- Home is visible in English with an empty search field.
- `control-azkarify doctor` reports `avd=AzkarifyVerify` and `package_installed=True`.

- **Confirm unfocused start.** Home is showing. Run `python3 .cursor/skills/verify-azkarify-android/scripts/control-azkarify wait --desc "Search azkar"` and `wait --text "Words of remembrance for morning and evening"`. Both exist. The field was not typed into yet.
- **Focus search.** Tap the field. Run `python3 .cursor/skills/verify-azkarify-android/scripts/control-azkarify tap --desc "Search azkar"`. The search field is the focused input.
- **Title match.** Type a sleeping-topic query. Run `python3 .cursor/skills/verify-azkarify-android/scripts/control-azkarify type --text "before sleeping"`. Wait with `wait --text "What to say before sleeping"`. That topic is visible. `wait --text "Words of remembrance for morning and evening" --absent` succeeds.
- **Submit.** Press Enter. Run `python3 .cursor/skills/verify-azkarify-android/scripts/control-azkarify key --code 66`. `What to say before sleeping` remains visible.
- **Empty state.** Replace the query by tapping `Clear search`, then type a miss. Run `tap --desc "Clear search"`, `tap --desc "Search azkar"`, `type --text "volcanoxyz"`. Wait for `wait --text "No results"`. The morning topic is absent.
- **Clear query.** Choose `Clear search`. Run `tap --desc "Clear search"`. `wait --text "Words of remembrance for morning and evening"` succeeds and `No results` is gone.
- **Proof.** Capture the populated match state (re-run the match steps if you cleared). Run `dump --path "$AZKARIFY_VERIFY_EVIDENCE/search-azkar/filtered.xml"` and `screenshot --path "$AZKARIFY_VERIFY_EVIDENCE/search-azkar/filtered.png"`. Both identify Azkarify, the query or result title `What to say before sleeping`, and the missing morning topic.

## Gotchas

- Cold start and window restore used to steal focus into search. If the keyboard is open before you tap search, the run is invalid; relaunch.
- Filtering is live on each character. Wait for the result title or `No results`, not a fixed sleep.
- Favorites search uses the same `Search azkar` handle but a different list. A miss on Favorites can mean the topic is not starred, not that search is broken.
- `input text` uses `%s` for spaces. Keep the helper's `--text` value in real spaces; do not pre-encode.
- Do not treat `./gradlew :app:connectedDebugAndroidTest` as a substitute for this dump/screenshot proof on an agent run.
