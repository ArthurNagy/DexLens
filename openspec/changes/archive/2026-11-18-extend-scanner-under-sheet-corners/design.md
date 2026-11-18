# Design

## Context

See proposal.md for why. Today's code in `:feature:pokedex:impl`:

- **`PokedexContent`** wraps everything in a `BoxWithConstraints` whose background is `CameraColors.background` (black)
  while the scanner is on. It calls `ScannerHome(windowHeight = maxHeight, areaHeight = scannerAreaHeight(...))`.
  In a wide window it sits in the list pane, with `topInset = 0`.
- **The sheet's shape.** `ScannerHome` gives the `BottomSheetScaffold` `sheetPeekHeight = windowHeight - areaHeight` and
  no `sheetShape`, so the sheet uses the default `BottomSheetDefaults.ExpandedShape`.
  - In Material 3 1.4.0 (the version the Compose BOM 2026.09.00 resolves) that shape is
    `SheetBottomTokens.DockedContainerShape` = `ShapeKeyTokens.CornerExtraLargeTop` =
    `MaterialTheme.shapes.extraLarge.top()`.
  - `AppTheme` does not override `shapes`, so the top corners are 28dp.
  - The sheet's top edge sits exactly at `areaHeight`, which is also `collapsedOffset` for the top bar fade.
- **The scaffold's body.** `BottomSheetScaffold` measures its body with loose constraints of the full layout height,
  places it at y = 0 and draws the sheet over it. So the body (`ScannerArea`) can be taller than `areaHeight`, and the
  sheet covers the extra part.
- **Pane layout.** `AppNavigation` uses `calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth`, so the home screen
  is one list pane (about 360dp wide) beside the detail placeholder from medium width up. That includes a phone in
  landscape (about 914 × 411dp) as well as tablets and unfolded foldables. Only compact windows, such as a phone in
  portrait, show home at full width.
- **`ScannerArea`** is one `BoxWithConstraints(height = areaHeight, clipToBounds, background = CameraColors.background)`.
  - It runs `aimBoxFor(...)` on the area's constraints.
  - Its children are the viewfinder slot, then `AimBoxOverlay` (a `fillMaxSize` scrim with the box cleared, carrying
    the `pokedex:aimBox` tag and the box's content description), then `ScanLabel` or the permission and message states,
    and last the fade layer.
  - Nothing of it is drawn behind the sheet's corners, so the outer black shows there: the wedges.
- **The aim box and the crop.**
  - `AimBox` holds fractions of the area. The viewfinder slot passes it to `FrameAnalyzer`, which calls
    `cropRegion(image.cropRect, rotation, aimBox())`.
  - `CameraViewfinder` builds a CameraX `ViewPort` with `FILL_CENTER` from its own constraints. The viewport is shared
    by `Preview` (default 4:3 selection) and `ImageAnalysis` (16:9 around 2560 × 1440).
  - CameraX 1.6.2 fits the viewport inside the intersection of the bound use cases' fields of view
    (`ViewPorts.calculateViewPortRects`). So `ImageProxy.cropRect` is exactly what the viewfinder shows.
  - That intersection, the picture both streams share, has an aspect between 4:3 and 16:9: it is whichever of the two
    streams is narrower on the sensor.

## Goals / Non-Goals

**Goals:**
- Show the camera behind the sheet's corners wherever that is free. "Free" means the preview keeps the scale and centre
  it has at the visible size, so the analysed sensor region is unchanged.
- Everywhere else, keep today's layout exactly: never a zoom, never a change to what is analysed.
- Decide before the camera binds, with a pure rule tests can drive, so the camera binds once at its final size.
- `CameraViewfinder`, `FrameAnalyzer` and `cropRegion` keep their code. In every window, the visible area's size, the
  aim box's position on screen, the label, the buttons and the message layouts are unchanged.

**Non-Goals:**
- No change to the sheet's shape, radius or peek height, or to the top bar fade's offsets. `areaHeight` keeps meaning
  "the visible area" everywhere it is used today.
- No change to `ListOnlyHome`, to the null state (the switch not read yet) or to when the camera starts and stops.

## Decisions

### 1. The radius comes from the sheet's real shape

- `ScannerHome` reads `val sheetShape = BottomSheetDefaults.ExpandedShape` once and passes it to
  `BottomSheetScaffold(sheetShape = sheetShape)`. That is the scaffold's default, so the sheet does not change, but the
  same object is now both drawn and measured.
- A small internal pure function `topCornerRadius(shape, size, layoutDirection, density): Float` calls
  `shape.createOutline(size, layoutDirection, density)` at the sheet's size: the window or pane width by
  `windowHeight - areaHeight`.
  - For an `Outline.Rounded` it returns `max(roundRect.topLeftCornerRadius.y, roundRect.topRightCornerRadius.y)`, read
    from `(outline as Outline.Rounded).roundRect`.
  - For any other outline it returns 0.
- `ScannerHome` gets `windowWidth = maxWidth` from `PokedexContent` and remembers the result as `cornerRadius: Dp`.

Alternatives considered:
- **A `28.dp` constant or `AppDimens` token.** It is a guess that drifts if the theme sets `shapes.extraLarge`.
- **Reading `MaterialTheme.shapes.extraLarge.topStart`.** It copies Material's internal token mapping instead of asking
  the shape the sheet draws.
- **Casting to `CornerBasedShape`.** It needs a cast and ignores how `createOutline` clamps radii, and the outline is
  what is actually drawn.

### 2. A deterministic rule, decided before binding

```kotlin
internal fun cornersFit(widthPx: Int, visibleHeightPx: Int, radiusPx: Int, uprightPortrait: Boolean?): Boolean =
    uprightPortrait != null &&
        (if (uprightPortrait) widthPx * 4f / 3f else widthPx * 9f / 16f) >= visibleHeightPx + 2 * radiusPx
```

- `widthPx` is the scanner area's width (its constraints). `visibleHeightPx = height.roundToPx()` and
  `radiusPx = cornerRadius.roundToPx()`, the same integers decision 3 lays out with.
- `uprightPortrait` says whether the camera picture, turned upright on the display, is taller than wide.
  `null` (unknown) never fills.

**Why it is safe.** The shared picture's aspect is between 4:3 and 16:9.
- Upright in portrait, a picture `w` wide is therefore at least `w × 4/3` tall at the width-limited scale.
- Upright in landscape, it is at least `w × 9/16` tall.
- If the rule says fill, the real picture is at least `h + 2r` tall. So a viewport `2r` taller is still width-limited:
  the scale stays the same, and decision 3 keeps the centre. The rule can only err towards not filling.

**Where `uprightPortrait` comes from: the camera's sensor orientation, not the window or the display's shape.**
- A new `scanner/CameraOrientation.kt` reads `SENSOR_ORIENTATION` from Camera2's `CameraManager` (synchronous, no
  permission needed, no CameraX start).
  - It reads it from the camera CameraX's `DEFAULT_BACK_CAMERA` picks: the first id in `getCameraIdList()` order whose
    `LENS_FACING` is `BACK` and whose `REQUEST_AVAILABLE_CAPABILITIES` contains `BACKWARD_COMPATIBLE`.
  - The result is cached once per process in a lazy holder (an `object` with a synchronized lazy value), so activity
    recreation and rotation don't repeat the binder calls.
  - It is looked up only when the scanner is on: `ScannerHome` calls it, and `ListOnlyHome` and the null state don't.
  - The read stays synchronous, so the decision is known before the viewfinder composes.
- It reads the display rotation from `LocalView.current.display?.rotation`, re-read when `LocalConfiguration` changes.
  - A null display (the view not attached yet) gives null, which means no fill.
  - It is keyed on the view's attach state so it re-reads once attached.
- It returns `((sensorOrientation - displayDegrees + 360) % 180) != 0`. That is the back camera's
  `getSensorRotationDegrees(displayRotation) % 180 != 0`: the buffer, which is always wider than tall, is turned a
  quarter to be upright.
- It returns null when there is no back camera or the lookup throws, and logs with Timber.
- A 180° flip doesn't change the result, so it is stable without a display listener.
- `PokedexScreen` passes a `cameraUprightPortrait: @Composable () -> Boolean?` provider to `PokedexContent`.
  `ScannerHome` invokes it, so the lookup only happens with the scanner on, and passes the value to `ScannerArea`.
  Tests and previews pass a constant.

Why not the display's orientation: on a book-style foldable, the inner display turned to portrait has the back camera's
long side horizontal. The display says portrait while the picture is landscape, so `w × 4/3` would over-estimate the
picture and the preview would zoom. The sensor orientation is exact for the camera CameraX binds by default (the first
back camera; back cameras on one device share an orientation in practice). A compact window on a landscape display is
handled correctly by both, because neither looks at the window's shape.

`ScannerArea` evaluates `cornersFit` on every composition. There is no saved state: after any recreation or resize the
same inputs give the same answer, so the viewfinder composes at its final size and binds once.

Results:

| Window | Visible area | Picture upright | Guaranteed height | Needs `h + 2r` | Corners |
|---|---|---|---|---|---|
| Phone portrait, full width (Pixel 7) | 411 × 298dp | portrait | 548dp | 354dp | camera |
| Phone landscape, 360dp list pane | 360 × 185dp | landscape | 202dp | 241dp | dark background |
| Landscape tablet or unfolded foldable, 360dp list pane | 360 × 240dp | landscape | 202dp | 296dp | dark background |
| Portrait tablet or foldable, 360dp list pane | 360 × 240dp | portrait | 480dp | 296dp | camera |
| Compact window on a landscape display | 400 × 291dp | landscape | 225dp | 347dp | dark background |

Alternatives considered (all rejected):
- **Measure the picture after binding and decide once** (the previous revision). It reads `ResolutionInfo` after the
  first bind and rebinds at the taller size. It costs a double bind on every phone cold start, a callback through the
  viewfinder slot, a change to `CameraViewfinder` and `rememberSaveable` state that must never re-evaluate.
- **Decide by the area's aspect ratio alone** (assume the picture's orientation from the window's shape). A portrait
  compact window on a landscape display would be assumed to have a portrait picture, so `w × 4/3` would be used where
  the picture is only `w × 9/16`, and the preview would zoom.
- **Decide by device type or window size class.** It is wrong both ways. A portrait tablet pane fills for free, and a
  "phone" in landscape is a list pane where filling would zoom.
- **Fill everywhere by enlarging the preview.** In a 360 × 240dp list pane the 16:9 picture fits the height exactly. A
  viewport 56dp taller needs the preview scaled by (240 + 56) / 240 = 1.23, about 66–69% of today's sensor pixels in
  the crop.
- **Hybrid: grow only downward in panes.** The scale is (240 + 28) / 240 = 1.12, about 80% of the pixels, and the
  centre moves. Both change recognition.
- **An extra `ResolutionInfo` check after binding that can only switch filling off.** It would bring back the
  callback, the `CameraViewfinder` change and a rebind on fallback. Since the rule is conservative across the whole
  4:3–16:9 range, the check is dropped.

### 3. When filling, the area grows under the sheet and the viewfinder grows at both ends

Pixel values come from integers computed once: `hPx = height.roundToPx()`, and
`rPx = if (cornersFit(...)) cornerRadius.roundToPx() else 0`.

The outer box is exactly `hPx + rPx` tall, still `clipToBounds` and `background(CameraColors.background)`. With
`rPx = 0` it is exactly today's area. It holds three layers:

1. **Camera layer.** It holds the viewfinder slot and `AimBoxOverlay`, which only draws and has no semantics.
   - A `layout` modifier measures it at exactly `hPx + 2 * rPx` and places it at `y = -rPx`.
   - Its top strip is clipped by the outer box.
     - On a phone in portrait it is above the window's top edge.
     - In a portrait tablet or foldable pane, which sits below `statusBarsPadding`, it is on screen under the status
       bar, so the clip must hold there.
   - Its bottom strip is behind the sheet's corners.
   - Both children get `aimBox.inViewfinder(hPx.toFloat(), rPx.toFloat(), rPx.toFloat())`.
2. **Content box.** It is exactly `hPx` tall, aligned to the top and tagged `pokedex:scannerArea`.
   - It holds `ScanLabel`, the permission and message states, and the aim box's semantics node (decision 4).
   - All of them are laid out against `hPx`.
3. **Fade layer.** It fills the outer box, so the strip under the sheet fades with the rest.

New pure function in `AimBox.kt`:

```kotlin
internal fun AimBox.inViewfinder(visibleHeight: Float, above: Float, below: Float): AimBox
// top' = (top * visibleHeight + above) / (visibleHeight + above + below), same for bottom; left and right unchanged
```

`aimBoxFor(...)` keeps running on the visible width and `hPx`. `ScannerHome` passes `height = areaHeight`, `cornerRadius`
and `cameraUprightPortrait`. `sheetPeekHeight`, `collapsedOffset`, `expandedOffset` and `stripHeight` keep
`areaHeight`.

Why the analysed region is unchanged when filling:
- The slot passes the remapped box to `FrameAnalyzer`. `CameraViewfinder` builds the viewport from its own constraints
  (`width × (hPx + 2rPx)`), so `cropRect` is exactly what the taller viewfinder shows. The overlay draws the same box,
  so what is cleared and what is analysed cannot disagree.
- Decision 2 guarantees the taller viewport is still width-limited, so the scale is the same.
- Its centre is at `-r + (h + 2r) / 2 = h / 2`, the visible area's centre. So every visible pixel maps to the same
  sensor pixel as at the visible size, and the crop is the same sensor region, up to cropRegion's two-pixel NV21
  rounding.
- The remapped box's bottom, `(bottom * h + r) / (h + 2r)`, is always above the visible edge at `(h + r) / (h + 2r)`.
  So no row behind the sheet or in the clipped top strip is ever in the crop.

When not filling, `rPx = 0`: `inViewfinder(h, 0, 0)` returns the box unchanged, the layout is today's, and so is the
bind.

In the permission, camera unavailable and scan error states, the camera layer is not composed. The corners show the
area's or the outer box's `CameraColors.background`, which are the same colour.

Alternatives considered:
- **Grow only the viewfinder.** The overlay and the fade would stop at `hPx`, so the corners would show undimmed,
  unfaded camera.
- **Grow the viewfinder only downward.** The centre moves by r/2, so the analysed region changes.

### 4. The aim box's description sits on a sibling sized to the visible box

`AimBoxOverlay` reaches `rPx` under the sheet. If it kept the content description, TalkBack's focus rectangle would
extend under the sheet.

- The `pokedex:aimBox` tag and `contentDescription` move to an empty `Box` in the content box, offset and sized to the
  visible aim box.
- It is composed only while the camera runs, as the overlay is today.
- Its bounds give tests the drawn box.
- `clearAndSetSemantics` is not used on the content box, because it would wipe `ScanLabel`'s polite live region.

### Form factors

One adaptive `:app` (phones, tablets, foldables). The change sits inside `ScannerArea`, and decision 2 decides per
window, as the table in decision 2 shows:

- Compact windows: phone portrait fills. A compact window on a landscape display does not.
- Medium and expanded windows: these are list panes. Phone landscape and landscape tablet or foldable panes do not
  fill. Portrait tablet or foldable panes do.

The entry pane is unaffected. The corners are symmetric, so RTL needs nothing extra.

## Risks / Trade-offs

- **[A device whose shared picture is outside 4:3–16:9]** → The streams' selectors make 4:3 and 16:9 the norm. The
  worst case is a slight zoom on such a device. The device checks cover a phone, a tablet and a foldable.
- **[The camera lookup costs main-thread time]** → It is two or three binder calls, made once per process and only
  with the scanner on. Task 5.1 measures cold start with the `:benchmarks` startup macrobenchmark or a Perfetto trace.
- **[CameraX's camera filtering changes in a future version]** → The lookup copies today's `DEFAULT_BACK_CAMERA`
  selection (first back, backward-compatible id). Back cameras share an orientation in practice anyway.
- **[The camera layer's top strip at `-rPx` is drawn by `CameraXViewfinder`'s EXTERNAL-mode `SurfaceView`, which
  Compose does not always clip]** → On phones that strip is off-window. In a portrait tablet or foldable pane it is on
  screen under the status bar, so the outer box's `clipToBounds` must hold for the SurfaceView. Task 5.1 checks
  exactly that pane on a device. If it does not clip, use a clip on the camera layer itself or the EMBEDDED
  implementation mode for that window.
- **[A future theme or Material update changes the corner radius]** → The radius is read from the shape the sheet
  draws. A test pins today's 28dp.
- **[The scrim and the fade also cover the strip under the sheet]** → Intended: the corners match the dimmed area and
  fade with it.
- **[Goldens move]** → Only the corner wedges change, in camera-on goldens where the rule fills.
  - The full-width tablet and foldable goldens in `PokedexScreenTest` (1280dp and 841dp wide) do not represent
    production, where home is a 360dp list pane there. The rule may fill them because they are wide.
  - New list-pane goldens pin the production layouts.
