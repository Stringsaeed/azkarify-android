# Favorites

Favorites lets a user star a topic from the Husn list, open the Favorites destination from the overflow menu, and see only starred topics there.

## Sub-features

- `favorite-add` stars a topic with `Add to favorites`.
- `favorite-list` shows that topic under `Menu` → `Favorites`.
- `favorite-remove` unstars with `Remove from favorites` and leaves Favorites empty if it was the only star.

## How to get to it (user POV)

- Tap the star on a category row (`Add to favorites` / `Remove from favorites`).
- Open `Menu` on Husn and choose `Favorites`.
- Use `Search azkar` on the Favorites list to filter starred topics.

## Driving it with control-azkarify

Preconditions:

- Azkarify is healthy on `AzkarifyVerify`.
- Favorites is empty (`No favorites yet` after opening Favorites), or you use a topic you will unstar during cleanup of fixture state.

- **Star a topic.** From Husn, tap add-to-favorites on a visible row. Run `tap --desc "Add to favorites"`. The control becomes `Remove from favorites`.
- **Open Favorites.** Run `tap --desc "Menu"` then `tap --text "Favorites"`. The starred topic title is visible.
- **Unstar.** Run `tap --desc "Remove from favorites"`. If it was the only favorite, `wait --text "No favorites yet"` succeeds.
- **Proof.** Capture Favorites while the starred topic is listed. Run `dump --path "$AZKARIFY_VERIFY_EVIDENCE/favorites/list.xml"` and `screenshot --path "$AZKARIFY_VERIFY_EVIDENCE/favorites/list.png"`. Artifacts show the Favorites title and the starred topic.

## Gotchas

- Stars persist in app preferences on the AVD. A leftover star from a crashed run invalidates the empty-state check; unstar or wipe the app: `adb -s emulator-5554 shell pm clear com.stringsaeed.azkarify` then relaunch (this also restores intro).
- The first `Add to favorites` match is the first on-screen star. Scroll the category list if the intended row is below the fold; dump first.
- Opening a topic from Favorites uses the same category title handle as Husn.
