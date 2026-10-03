# Adaptive Android layout verification

Verified on 3 October 2026 using Android 17 / API 37 emulators.

The application uses Material Adaptive `ListDetailPaneScaffold` and the standard
`calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth` directive. Medium and
expanded windows show list and detail together; compact windows show the current
pane. Each pane has an Android Material top app bar. The topic list retains search
and highlights the selected category. Topic changes replace the current selection
rather than adding every list tap to the back stack.

The library avoids separating vertical hinges. Tabletop posture uses its native
vertical reflow strategy, with detail above the list. Android resize support is
explicit in the activity manifest. No orientation lock is imposed.

## Automated checks

All five UI tests pass on each of these devices, for 15 passing runs:

| Emulator | Tested initial window | UI checks |
| --- | --- | --- |
| Pixel 10a | Phone portrait, compact | 5 passed |
| Pixel Tablet | Tablet portrait, medium | 5 passed |
| Pixel Fold | Unfolded, expanded | 5 passed |

Checks cover topic selection and pane visibility; search and clearing; favorites and
reading; counter reset and restoration; Arabic/dark preferences; journey progress;
and selection/query persistence across activity recreation and back navigation.
The 11 existing journey and prayer unit tests also pass. Debug and release builds pass.

XML reports are saved in `../adaptive-test-results/`.

## Device transitions

Screenshots in this folder record selected-topic continuity through rotation,
folding, unfolding, and viewport resizing. A resized emulator viewport checks the
same window-size adaptation used by multi-window mode; it does not replace a test
of an actual OS split-screen session or a physical folding device.

Native emulator console commands are used for posture and rotation. Emulator
viewport overrides are restored after the narrow-width check.

These are emulator layout and behavior checks, not physical-device performance
measurements. OEM-specific fold behavior still needs device verification.

The same five UI flows also pass on the half-open, rotated Fold in tabletop
posture, for 20 UI test runs in total. The tabletop topic list omits the overview
card so topics are immediately available. Journeys remains accessible through the
Android overflow menu. `tabletop.xml` records this additional run.

`window-cases.json` records nine live layout observations, including physical
screen dimensions and visible list/detail content. The selected wake-up topic
remained visible in every transition. `tablet-narrow.png` shows the 480 dp viewport;
`fold-closed.png` shows the cover screen; `fold-tabletop.png` shows stacked panes.

To capture a foldable without screencap's multiple-display warning corrupting the
binary output, use:

```sh
python3 scripts/capture_android_screenshot.py emulator-5558 output.png
```
