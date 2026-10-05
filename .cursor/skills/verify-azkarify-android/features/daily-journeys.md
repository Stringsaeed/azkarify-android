# Daily journeys

Daily journeys lets a user open the journey list, start Heading out, mark its required leaving-home step complete, and see journey completion plus awarded points.

## Sub-features

- `journey-open-list` opens journeys from the overflow menu or the home overview card.
- `journey-open-heading-out` opens `Heading out`.
- `journey-complete-step` marks `Leaving home azkar` complete.
- `journey-points` shows `Journey complete` and ` 35 ` in the detail pane on first full completion of that journey.

## How to get to it (user POV)

- Tap `Explore journeys` / the `Your daily journeys` card on Husn when the overview is showing (not tabletop, keyboard hidden).
- Or tap `Menu` then `Journeys`.
- Tap `Heading out`, then the complete control `Mark complete: Leaving home azkar`.

## Driving it with control-azkarify

Preconditions:

- Azkarify is healthy on `AzkarifyVerify`.
- Heading out is not already complete for today, or you accept that re-completing must not increase points.

- **Open list.** From Husn, run `tap --desc "Menu"` then `tap --text "Journeys"`. `wait --text "Heading out"` succeeds.
- **Open Heading out.** Run `tap --text "Heading out"`. `wait --desc "Mark complete: Leaving home azkar"` succeeds.
- **Complete the step.** Run `tap --desc "Mark complete: Leaving home azkar"`. `wait --text "Journey complete"` succeeds.
- **Points.** Dump or screenshot the detail pane. The points control includes ` 35 ` after this first completion of the one-step journey (10 for the step + 25 for finishing).
- **Idempotent award.** Tap incomplete then complete again. Run `tap --desc "Mark incomplete: Leaving home azkar"` then `tap --desc "Mark complete: Leaving home azkar"`. Points stay ` 35 `.
- **Proof.** Run `dump --path "$AZKARIFY_VERIFY_EVIDENCE/daily-journeys/complete.xml"` and `screenshot --path "$AZKARIFY_VERIFY_EVIDENCE/daily-journeys/complete.png"`. Artifacts show `Journey complete` and the points value.

## Gotchas

- Progress is per calendar day on the device. A second run the same day will already be complete; unmark first or treat existing `Journey complete` as blocked, not as a fresh award proof.
- Optional steps do not finish a journey. Heading out is the small fixture because it has one required step.
- Overview card is omitted in tabletop posture; use `Menu` → `Journeys` there.
- Do not claim iOS journey UI as proof.
