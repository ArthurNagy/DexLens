# Design

## Context

See proposal.md for why. Today `PokedexScreen` and `ScannerScreen` are separate Nav 3 entries in separate features, each with
its own ViewModel. The project rules forbid one feature's impl from using another's, so a camera can't simply be placed
inside the list screen. The scanner's parts are already well separated: `AimBox` math, `CameraViewfinder`, `TextFrameAnalyzer`,
and a pure `ScannerState.afterFrame` reducer. Most of them move unchanged.

This change touches `:app` only as a form factor: phones, foldables and tablets. On windows from 600dp the home screen is
already the list pane of the list-detail scene, so it sees a phone-like width there too.

## Goals / Non-Goals

**Goals:**
- One screen, one ViewModel and one state decide what the list shows.
- The camera is genuinely off, not just hidden, whenever it can't be seen.

**Non-Goals:**
- A custom sheet implementation, or animating the camera area's size.
- Any change to the matcher or `LockOnTracker`.

## Decisions

### 1. The scanner joins `:feature:pokedex:impl`
Camera code moves to a `scanner` subpackage of `:feature:pokedex:impl`, and `:feature:scanner:{api,impl}` are deleted. String
keys become `pokedex_scanner_*` to satisfy the module's `resourcePrefix`, and test tags become `pokedex:*`.

Alternative: keep `:feature:scanner` and expose the camera panel as a Hilt-bound composable from its `:api`. That isolates the
camera dependencies, but `:api` modules hold only Nav keys today, and it would add a new cross-feature pattern for one screen.
Recorded in ADR 0013.

### 2. One ViewModel, with scan state nested in `PokedexState`
`PokedexViewModel` takes over `ScannerViewModel`'s use cases and intents. `ScannerState` becomes a nested `ScanState` value
holding the matcher, lock history, match, torch and debug data, and it keeps `afterFrame` as a pure reducer, so most of its
tests carry over. `artwork` and the species map stop being duplicated.

Alternative: two ViewModels bridged by the screen. That needs state copied between them through a `LaunchedEffect`, and leaves a
window where typed text and a late match disagree. A frame only emits new state when candidates change, because idle frames
collapse to `LockOnTracker.Empty`, so sharing one state costs almost nothing while no name is in view.

### 3. A match is separate from search results
`results` keeps meaning "the answer to `query`", so the full list stays loaded and clearing a match is instant. The list renders
a derived `listed` value: the match mapped to `SpeciesSearchResult` when the query is blank and a match exists, otherwise
`results`. `onQueryChanged` with non-blank text clears the match and the lock history in the same reduce.

Alternative: write the match into `results`. Clearing would then need a new search and a loading state.

### 4. Locking while continuing to scan
`afterFrame` stops returning early on a match. A lock resets the tracker, and it only replaces the match and sets
`pendingHaptic` if it contains a species outside the current match. So a name staying in view doesn't buzz every few frames, and
a battle screen flickering between one and two names can't shrink the list. Frames are ignored while `query` is non-blank, which
covers frames already in flight when typing starts. `paused`, `onResultDismissed` and `isPaused` are removed.

### 5. Material 3 `BottomSheetScaffold`, inset from the top
The screen is a `BoxWithConstraints` on `CameraColors.background`, stacked in this order:
1. The scanner area, a fixed height at the top.
2. A `BottomSheetScaffold` with no hidden state and full `sheetMaxWidth`, with its top padded by the strip height: status bar
   plus the button row. That padding is what stops the expanded sheet short of the settings button.
3. The button row, at the top end.

Peek height is the window height minus the scanner area. The area's height is `min(width × 2/3 + top inset, height × 0.45)`, so it
is 3:2 below the status bar on phones and capped in short windows such as landscape phones. Expanding simply covers the area, so
the viewfinder's size never changes and CameraX never rebinds for a size change.

The search field is the collapsed form of M3 `SearchBar` with today's `InputField`, without `TopSearchBar`. The trailing icon is
the clear button, shown only when there is typed text or a match. The list gets `imePadding` so its last rows clear the keyboard.

Alternative: a custom `AnchoredDraggable` sheet. It gives more control, but re-implements nested scrolling and accessibility
actions that the scaffold already has.

### 6. The camera exists only while it can run
`CameraViewfinder` and the recogniser are composed only when the sheet's `currentValue` and `targetValue` are both
`PartiallyExpanded`, permission is granted, and the camera and matcher haven't failed. Leaving composition already calls
`unbindAll()`, which turns off the camera and its privacy indicator. Phones also get "covered by an entry" for free, because the
list entry leaves composition. The existing torch effect re-applies `torchOn` when the camera comes back. The torch button shows
only while the viewfinder is composed.

The `camera` slot stays on the stateless content so tests and goldens run without hardware. A test can assert the slot leaves
composition when the sheet expands.

### 7. Sheet position drives search mode
Sheet position stays in `SheetState`, not in the ViewModel, because nothing outside the composable needs it. Focus on the field
calls `expand()`. A `snapshotFlow` on `targetValue` reaching `PartiallyExpanded` clears the text, clears focus and hides the
keyboard, so a drag, a list fling and Back all end a search the same way. `rememberTextFieldState` and the sheet state are both
saveable, which keeps a search across an entry visit.

Back uses one `NavigationBackHandler`, enabled when the sheet is expanded or a match is listed, and never while an entry is open
beside the list. Expanded collapses first, otherwise it clears the match.

### 8. Near-square aiming box that avoids the controls
`aimBoxFor` takes the top inset and the width reserved for the button row, and returns fractions as before. The box is centred
horizontally, centred in the space below the status bar, about 80% of that height, and no wider than the space left clear of the
buttons. That is roughly 205 × 195 dp on a Pixel 6. The hint and the matched names share one label just inside the box's bottom
edge, marked as a polite live region.

The camera's 3:2 view sits inside a portrait 9:16 frame, so the frame's full width still spans the screen, and pixel density on
a name is at least what the full-screen scanner had.

### 9. Status bar
On compact widths the camera runs behind the status bar, so the existing "light icons" effect moves up to the screen and applies
for as long as the screen is composed. From 600dp the status bar also spans the detail pane, where light icons would vanish, so
there the content starts below the status bar and the effect is not applied.

### 10. Permission and error messages fit the area
`PermissionPrompt`, "no camera" and matcher-error messages render inside the scanner area on the dark background, in a
`verticalScroll` so they stay reachable when the area is capped to a short height.

## Risks / Trade-offs

- [The camera starts on every launch, using battery and showing the privacy indicator] → It runs only while visible, and the README says so.
- [A larger box reads more text, so a game's Pokédex list could lock several names at once] → The list handles many matches, and the 3-of-5 rule is unchanged. Check on real game screens during QA, and tighten `aimBoxFor` if needed.
- [A larger crop makes recognition slower] → `STRATEGY_KEEP_ONLY_LATEST` drops frames instead of queueing them. The debug overlay shows milliseconds per frame.
- [A fling down at the top of the list collapses the sheet and clears typed text] → Accepted, for one consistent rule: collapsed means scanning.
- [The list can change while reaching for a row, if the camera catches another name] → A new lock needs 3 of 5 frames and must add a species.
- [`BottomSheetScaffold` measures with the IME in ways that vary by M3 version] → Covered by a device check of keyboard behaviour in QA.

## Migration Plan

No data migration. A saved back stack containing `ScannerKey` can't survive an app update, so removing the key is safe. The model
file publishing path changes to `feature/pokedex/impl/src/main/assets/`. To roll back, revert the PR.
