# Proposal

Issue: #31

## Why

With the scanner on and the list sheet collapsed, the camera preview stops exactly where the sheet starts. The sheet's
top corners are rounded, so a black wedge shows on the left and on the right where the camera should be. On a phone in
portrait it is the first thing people see on every launch and makes the join between the scanner and the list look
broken. The preview should carry on under the sheet's top edge so the rounded corners show the live camera, wherever
that is free. The visible scanner area, its controls and the part of the camera picture that is recognised must not
change anywhere, and the preview is never enlarged.

## What Changes

- The live preview shows behind both of the sheet's rounded top corners where the camera picture is guaranteed to
  reach past the visible scanner area by the corner radius above and below at its current scale.
  - The rule uses the scanner area's width and the camera's orientation relative to the display. It is decided before
    the camera starts.
  - It holds on phones in portrait and in the list pane of a portrait tablet or foldable.
- Where filling the corners would need the preview enlarged, the preview is not enlarged and the corners show the
  scanner area's dark background. That is a phone in landscape (its home screen is a 360dp list pane), the list pane of
  a landscape tablet or unfolded foldable, and compact multi-window on a landscape display.
- The part of the scanner area you can see keeps its size (3:2 below the status bar, or the 45% cap). The aiming box,
  the hint or match label, the torch and the settings button end above the sheet's top edge.
- The analysed sensor region never changes, and none of the preview hidden under the sheet is sent to recognition.
- Where the camera shows behind the corners, that area fades toward the top bar colour with the rest of the scanner
  area while the sheet moves, with no black flash at the corners.
- In the permission prompt, "camera unavailable" and scan error states the corners show the scanner area's dark
  background. With the scanner switched off there is no camera and the list starts right below the top bar.

## Non-goals

- No change to the sheet's shape, corner radius, peek height or drag behaviour, or to the scanner area's size rules
  (3:2 or the 45% cap)
- No change to when the camera starts or stops (the sheet expanded, home covered, scanner switched off)
- No change to the switched-off layout (`ListOnlyHome`) beyond confirming it is unchanged
- No new module, library or domain/data change. `:core:domain` and `:core:data` stay untouched
- No zooming of the preview to fill the corners, in any window

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `home-screen`: the "Scanner above a list sheet" requirement changes in two ways.
  - The camera preview carries on behind the sheet's rounded top corners where the area's width, at the camera's
    orientation, guarantees that is free.
  - The analysed region never changes.
  - It gains scenarios for each acceptance criterion of #31.

## Impact

- `:feature:pokedex:impl` only:
  - `PokedexScreen.kt`: `ScannerHome` reads the sheet's real shape and passes its top corner radius down.
    `PokedexContent` takes the camera's upright orientation as a parameter, which `PokedexScreen` reads from the back
    camera's sensor orientation and the display rotation.
  - `scanner/ScannerArea.kt`: applies the rule. Where it fills, the area draws the camera, the aim-box overlay, its
    background and the fade over an extra strip under the sheet, while its content keeps the visible height.
  - `scanner/AimBox.kt`: pure functions for the rule and for re-expressing the aiming box in a taller viewfinder.
  - `scanner/CameraOrientation.kt` (new): the camera's upright orientation.
  - `CameraViewfinder`, `FrameAnalyzer` and `cropRegion` keep their code.
- Tests in `:feature:pokedex:impl`:
  - unit tests for the rule, the orientation, the box mapping and the corner radius
  - screen tests for the corner pixels, the analysed region, filled and unfilled list panes, and the layout
  - re-recorded home goldens
- No new dependency, no API change outside the module, no ADR. The change uses Material 3's `BottomSheetDefaults` and
  the platform's camera characteristics, both already available to the module.
