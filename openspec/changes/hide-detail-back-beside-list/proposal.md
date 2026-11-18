# Proposal

Issue: #30

## Why

On a tablet or an unfolded foldable, a detail pane shown beside its list draws its own Back arrow next to the list's.
Under Settings → Open-source licences, the licence's arrow clears the licence to the placeholder while the list's arrow
returns to Settings; the species entry beside the home list shows the same extra arrow. Two arrows that do different
things side by side are confusing, and the detail's arrow adds nothing while the list stays visible. Found by
`android-qa` while verifying #23 on a tablet emulator.

## What Changes

- A library's licence and a species entry hide their top-bar Back arrow while they show beside their list. On a phone
  in portrait, or whenever the detail fills the window on its own, the arrow shows as today and goes back.
- When the window changes size (fold, unfold, resize) with a detail open, the arrow follows the layout: it appears once
  the detail fills the window on its own and disappears once it shows beside it, and the open library or species stays the same.
- The signal "is this detail shown beside its list?" comes from the list-detail scene the adaptive Navigation 3 scene
  strategy draws, not from a separate window-width check, through one small shared helper in `:core:ui`.
- The licences list keeps its Back control, and the entry's "can't be found" message keeps its own Back action on every
  window size.

## Non-goals

- No change to what system Back, predictive back or the list's Back control do on any window size
- No change to the placeholder, the pane widths or the phone layout
- No new feature module, and no change to `:core:domain` or `:core:data`
- Other screens with no list-detail layout are not affected

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `open-source-notices`: "Licences layout" adds that the licence shows a Back arrow only while it fills the window on
  its own, with scenarios for the wide window, a phone in portrait and a window size change.
- `species-entry`: "Adaptive layout" adds that the entry shows a Back arrow only while it fills the window on its
  own, with scenarios for the wide window, a phone in portrait and a window size change.

## Impact

- `:core:ui`: a new composable helper that reads the adaptive Navigation 3 list-detail scene scope; `:core:ui` gains an
  `implementation` dependency on `androidx.compose.material3.adaptive:adaptive-navigation3` (already in the catalog and
  already on every feature impl through the `android-forge.feature.impl` convention), and a test-only dependency on
  `navigation3-ui` for its scene test. No new library, so no ADR.
- `:feature:settings:impl`: `SettingsEntries` hides the licence's Back when it shows beside the list.
- `:feature:entry:impl`: `EntryContent` / `EntryScreen` take whether to show the top-bar arrow; `EntryEntries` passes it.
- `:app`: `AppNavigationTest` stand-ins follow the entries; no production change in `AppNavigation`.
- Goldens: tablet and foldable goldens of both detail screens are re-recorded without the arrow; phone goldens stay.
- Form factors: phones, tablets and foldables of the one adaptive `:app`. No other form factor is affected.
