# Read and count

Read and count lets a user open a topic, read an entry, start a countdown from a repetition chip, tap the number down, reset it, and use the standalone counter from the overflow menu.

## Sub-features

- `read-open` opens a topic from the Husn list into the entries pane.
- `read-counter-chip` starts a countdown from a `📿  N` chip.
- `read-count-tap` decrements `Count: N` by one tap.
- `read-reset` restores the original count.
- `counter-standalone` opens `Menu` → `Counter` for a count-up from zero.

## How to get to it (user POV)

- Tap a topic title on Husn, for example `Words of remembrance for morning and evening`.
- On an entry with repetitions, tap `📿  3` (content description `Count down from 3`).
- Tap the large number (`Count: 3`) to count down; tap `Reset` to restore.
- Open `Menu` then `Counter` for the untitled counter.

## Driving it with control-azkarify

Preconditions:

- Azkarify is healthy on `AzkarifyVerify`.
- Home search is empty.
- `control-azkarify doctor` reports the verification AVD.

- **Open topic.** Choose the morning topic. Run `tap --text "Words of remembrance for morning and evening"`. Wait until entries are listed; a `📿  3` chip exists (`wait --contains "3"` after dump, or tap via `--text "📿  3"` if the dump shows that exact text).
- **Start countdown.** Tap the first three-repetition chip. Run `tap --text "📿  3"` (or `tap --desc "Count down from 3"` if dump exposes it). `wait --desc "Count: 3"` succeeds.
- **Count down one.** Tap the number. Run `tap --desc "Count: 3"`. `wait --desc "Count: 2"` succeeds.
- **Reset.** Choose `Reset`. Run `tap --text "Reset"`. `wait --desc "Count: 3"` succeeds.
- **Standalone counter.** Go back to home (`tap --desc "Back"` as needed), open overflow, choose Counter. Run `tap --desc "Menu"`, `tap --text "Counter"`. The screen shows `Tap the number to count up`.
- **Proof.** Capture countdown after reset. Run `dump --path "$AZKARIFY_VERIFY_EVIDENCE/read-and-count/count.xml"` and `screenshot --path "$AZKARIFY_VERIFY_EVIDENCE/read-and-count/count.png"`. Artifacts show the zikr text or `Count: 3`.

## Gotchas

- Compact phones hide the list after a topic tap; use `Back` to return. Medium/expanded panes keep the list.
- Counter state is in the activity. Killing the emulator clears it; that is not a persistence bug for the standalone counter.
- Do not use `Count: ` with a trailing space as the full content description; the on-screen value is `Count: 3` with the formatted number.
- Audio beeps are not required proof.
