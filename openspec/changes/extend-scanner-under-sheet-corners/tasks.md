# Tasks

All work is in `:feature:pokedex:impl`. No `:core:*`, `:feature:*:api` or `:app` code changes; the `:app` checks are
verification only. Pixel values in tests use the same `roundToPx()` integers the code uses. Production shows home as a
360dp list pane from medium width up (phone landscape, tablets, unfolded foldables), so every non-compact layout test
below wraps `PokedexContent` in `Box(Modifier.width(360.dp))`.

## 1. Pure geometry (:feature:pokedex:impl, scanner/AimBox.kt, scanner/CameraOrientation.kt, scanner/SheetCorners.kt)

- [ ] 1.1 :feature:pokedex:impl — add the pure functions:
  - In `scanner/AimBox.kt`: `AimBox.inViewfinder(visibleHeight, above, below)` and `cornersFit(widthPx: Int, visibleHeightPx: Int, radiusPx: Int, uprightPortrait: Boolean?)`, which is `uprightPortrait != null && (if (uprightPortrait) widthPx * 4f / 3f else widthPx * 9f / 16f) >= visibleHeightPx + 2 * radiusPx`. Update `AimBox`'s KDoc to say its fractions are of the surface it is drawn on.
  - In a new `scanner/CameraOrientation.kt`: `uprightPortrait(sensorOrientationDegrees: Int, displayRotationDegrees: Int): Boolean`, which is `((sensor - display + 360) % 180) != 0`.
  - Verify `./gradlew :feature:pokedex:impl:compileDebugKotlin` passes.
- [ ] 1.2 :feature:pokedex:impl — rule tests in `AimBoxTest`, with `rPx = 28dp.roundToPx()` at each case's density:
  - (a) `// Scenario: Camera behind the sheet corners`: `cornersFit` is true for a Pixel 7 portrait area (411 × 298dp at 2.625, `uprightPortrait = true`) and for a portrait tablet or foldable 360 × 240dp list pane (density 2, `true`).
  - (b) `// Scenario: Corners where filling would enlarge the preview`: `cornersFit` is false for:
    - a phone landscape 360dp list pane (360 × 185dp at 2.625, `false`)
    - a landscape tablet or foldable 360 × 240dp list pane (density 2, `false`)
    - a compact 400 × 291dp window on a landscape display (density 2, `false`); the same numbers with `true` would fill, which is why the camera's orientation, not the window's shape, is the input
    - any case with `uprightPortrait = null`
    - Also add an exact-boundary case (equal heights fill, one pixel short does not).
  - (c) `// Scenario: Preview under the sheet not analysed`: use a test helper that simulates CameraX `FILL_CENTER` for a shared picture of aspect 16:9 and of 4:3. `inViewfinder(h, 0f, 0f)` returns the box unchanged. For every case where `cornersFit` is true, and for both aspects:
    - `cropRegion` over the visible viewport's crop with the visible box equals `cropRegion` over the `hPx + 2rPx` viewport's crop with `box.inViewfinder(hPx, rPx, rPx)`, within 2 pixels on every edge
    - the region's rows stay strictly inside the rows of the visible part of the viewport
  - Verify `./gradlew :feature:pokedex:impl:testDebugUnitTest --tests '*AimBoxTest*'` passes.
- [ ] 1.3 :feature:pokedex:impl — `CameraOrientationTest` tagged `// Scenario: Camera behind the sheet corners` and `// Scenario: Corners where filling would enlarge the preview`: `uprightPortrait` is true for sensor 90 at display 0 and 180, and for sensor 0 at display 90 and 270. It is false for sensor 90 at display 90 and 270, and for sensor 0 or 180 at display 0. Sensor 270 mirrors sensor 90. Verify the test passes
- [ ] 1.4 :feature:pokedex:impl — add `topCornerRadius(shape: Shape, size: Size, layoutDirection: LayoutDirection, density: Density): Float` in a new `scanner/SheetCorners.kt`. It calls `shape.createOutline(...)`, takes `max(roundRect.topLeftCornerRadius.y, roundRect.topRightCornerRadius.y)` of `(outline as Outline.Rounded)`, and returns 0 for any other outline. Unit tests tagged `// Scenario: Camera behind the sheet corners`: `RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)` gives 28dp in pixels at density 2.625 in both layout directions, an asymmetric top shape gives the larger radius, and `RectangleShape` gives 0. Verify `./gradlew :feature:pokedex:impl:testDebugUnitTest --tests '*SheetCorners*'` passes

## 2. Camera orientation source (:feature:pokedex:impl, scanner/CameraOrientation.kt)

- [ ] 2.1 :feature:pokedex:impl — add `@Composable rememberBackCameraUprightPortrait(): Boolean?`:
  - It reads `SENSOR_ORIENTATION` from `CameraManager`, synchronously, for the camera CameraX's `DEFAULT_BACK_CAMERA` picks: the first id in `getCameraIdList()` order with `LENS_FACING_BACK` and `BACKWARD_COMPATIBLE` in `REQUEST_AVAILABLE_CAPABILITIES`.
  - It caches the result once per process in a lazy holder `object`, so recreation and rotation don't repeat the binder calls.
  - It takes the display rotation from `LocalView.current.display?.rotation` (`Surface.ROTATION_*` mapped to degrees), keyed on `LocalConfiguration` and on the view's attach state. A null display gives null (no fill) and is re-read once attached.
  - It returns `uprightPortrait(...)`, or null with a `Timber.w` when there is no such camera or the lookup throws.
  - Add a Robolectric test using `ShadowCameraManager`:
    - a compatible back camera with orientation 90 gives true at rotation 0
    - a non-`BACKWARD_COMPATIBLE` back camera listed first (orientation 0) is skipped for a compatible back camera listed second (orientation 90), which gives true
    - no camera gives null
    - a second lookup reuses the cached orientation
  - Verify `./gradlew :feature:pokedex:impl:testDebugUnitTest --tests '*CameraOrientation*'` passes.

## 3. The scanner area (:feature:pokedex:impl, scanner/ScannerArea.kt)

- [ ] 3.1 :feature:pokedex:impl — give `ScannerArea` `cornerRadius: Dp` and `cameraUprightPortrait: Boolean?` parameters (the viewfinder slot's type is unchanged). Then:
  - On every composition, compute `hPx = height.roundToPx()`, `fit = cornersFit(constraints.maxWidth, hPx, cornerRadius.roundToPx(), cameraUprightPortrait)` and `rPx = if (fit) cornerRadius.roundToPx() else 0`. Use no saved state.
  - The outer box is exactly `hPx + rPx` pixels tall (still `clipToBounds` and `background(CameraColors.background)`).
  - A camera layer is measured at exactly `hPx + 2 * rPx` and placed at `-rPx` by a `layout` modifier. It holds the slot and `AimBoxOverlay`, both given `aimBox.inViewfinder(hPx.toFloat(), rPx.toFloat(), rPx.toFloat())`.
  - `AimBoxOverlay` loses its tag and semantics.
  - A top-aligned content box of exactly `hPx`, tagged `pokedex:scannerArea`, holds `ScanLabel` (padding from the visible box and `hPx`), `PermissionPrompt` and `ScannerMessage`. While the camera runs, it also holds an empty `Box` offset and sized to the visible aim box, carrying the `pokedex:aimBox` tag and the aim box's `contentDescription`. Do not use `clearAndSetSemantics` on the content box.
  - The fade layer stays the last child and fills the outer box.
  - Update `ScannerArea`'s KDoc.
  - Verify `./gradlew :feature:pokedex:impl:compileDebugKotlin` passes.
- [ ] 3.2 :feature:pokedex:impl — a Robolectric `ScannerAreaTest` (`@GraphicsMode(NATIVE)`, `Pixel7`) rendering `ScannerArea` directly with `cornerRadius = 28.dp` and a solid-colour viewfinder stub. The stub records the `AimBox` it receives and its own unclipped bounds. "The drawn box" is that `AimBox` laid onto the stub's bounds; it must equal the `pokedex:aimBox` node's bounds within 1px.
  - `// Scenario: Corner preview keeps the scanner layout`: with `cameraUprightPortrait = true`, the stub's bounds are `pokedex:scannerArea`'s bounds grown by 28dp at the top and at the bottom. `pokedex:scannerArea`, `pokedex:scanLabel` and the drawn box have the same bounds as with `false`.
  - `// Scenario: Corners where filling would enlarge the preview`: with `false` and with `null`, the stub's bounds equal `pokedex:scannerArea`'s bounds and the outer box is exactly `height` tall.
  - `// Scenario: Sheet corners fade with the camera`: with `true` and `topBarAlpha = { 0.5f }`, a strip pixel below `hPx` equals the pixel just above `hPx` at the same x (both outside the aim box). With `{ 1f }`, it equals `surfaceContainer`, never `CameraColors.background`.
  - `// Scenario: Scanner messages fill the sheet corners`: with the permission prompt, `camera.failed` and a scan error, the pixel below `hPx` equals `CameraColors.background` and the message is centred in `pokedex:scannerArea`.
  - Verify `./gradlew :feature:pokedex:impl:testDebugUnitTest --tests '*ScannerAreaTest*'` passes.

## 4. The home screen (:feature:pokedex:impl, PokedexScreen.kt, PokedexPreviews.kt)

- [ ] 4.1 :feature:pokedex:impl — wire up the home screen:
  - In `ScannerHome`, read `val sheetShape = BottomSheetDefaults.ExpandedShape` and pass it as `BottomSheetScaffold(sheetShape = sheetShape)`.
  - Compute `cornerRadius` with `topCornerRadius(sheetShape, Size(windowWidth, windowHeight - areaHeight)` in pixels, `LocalLayoutDirection.current`, `LocalDensity.current)`, remembered on its inputs.
  - Add `windowWidth: Dp`, fed with `maxWidth`. Add `cameraUprightPortrait: @Composable () -> Boolean?` to `PokedexContent` and `ScannerHome`. `ScannerHome` invokes it (so the camera lookup happens only with the scanner on) and passes the value to `ScannerArea`. `PokedexScreen` passes `{ rememberBackCameraUprightPortrait() }`.
  - `sheetPeekHeight`, `collapsedOffset`, `expandedOffset` and `stripHeight` keep `areaHeight`.
  - Update `PokedexPreviews.kt` to pass `true`. `ListOnlyHome` is unchanged.
  - Verify `./gradlew :feature:pokedex:impl:compileDebugKotlin` passes.
- [ ] 4.2 :feature:pokedex:impl — in `PokedexScreenTest`:
  - `show(...)` gains `uprightPortrait: Boolean?`, defaulting to `true` in portrait test windows and `false` in landscape ones (as a back camera with sensor orientation 90 reports). Add a `pane: Boolean` option that wraps `PokedexContent` in `Box(Modifier.width(360.dp))`.
  - `ViewfinderStub` records the `AimBox` it receives and its unclipped bounds.
  - Update `scannerSitsAboveTheSheet` (`// Scenario: Home shows scanner and sheet`): the 3:2 check and `search.top >= area.bottom` use `pokedex:scannerArea`, and the stub's bounds are the area grown by 28dp at the top and at the bottom.
  - Verify the test passes.
- [ ] 4.3 :feature:pokedex:impl — a Robolectric test pinning the radius source (`// Scenario: Camera behind the sheet corners`): inside `AppTheme`, `topCornerRadius(BottomSheetDefaults.ExpandedShape, ...)` is 28dp in pixels. Verify the test passes
- [ ] 4.4 :feature:pokedex:impl — corner pixel tests tagged `// Scenario: Camera behind the sheet corners`, on `Pixel7` (full width) and on a portrait tablet `w800dp-h1280dp-xhdpi` in a 360dp pane: with the camera running and the sheet collapsed, the pixel 2px in from each side and 2px below `pokedex:scannerArea`'s bottom is not `CameraColors.background` and equals the pixel 2px above that bottom at the same x. Add a `tabletPortraitPane` golden. Verify the tests pass
- [ ] 4.5 :feature:pokedex:impl — no-fill tests tagged `// Scenario: Corners where filling would enlarge the preview` and `// Scenario: Preview under the sheet not analysed`, on three layouts:
  - a phone in landscape (`w914dp-h411dp-xxhdpi`) in a 360dp pane
  - `MediumTablet` in a 360dp pane
  - a compact `w400dp-h800dp-xhdpi` window with `uprightPortrait = false` (a landscape display)

  In each:
  - the stub's bounds equal `pokedex:scannerArea`'s bounds
  - the corner pixel below the area's bottom at each side equals `CameraColors.background`
  - the `AimBox` the stub received equals `aimBoxFor(...)` on the visible area exactly, so the analysed region is unchanged

  Keep the `tabletListPane` golden (MediumTablet, 360dp pane) and add `phoneLandscapePane`. Verify the tests pass.
- [ ] 4.6 :feature:pokedex:impl — analysed-region test tagged `// Scenario: Preview under the sheet not analysed`, on `Pixel7` and the portrait tablet pane: the `AimBox` the stub received, laid onto the stub's unclipped bounds, equals the `pokedex:aimBox` node's bounds within 1px, and its bottom edge is above `pokedex:scannerArea`'s bottom (the sheet's edge). Verify the test passes
- [ ] 4.7 :feature:pokedex:impl — layout and scanner-off tests:
  - `// Scenario: Corner preview keeps the scanner layout`: on `Pixel7` and the portrait tablet pane, `pokedex:scannerArea`'s height equals `scannerAreaHeight(...)`, and `pokedex:aimBox`, `pokedex:scanLabel`, `pokedex:torch` and `pokedex:settings` all end above the sheet's top edge.
  - `// Scenario: Scanner off has no corner gap`: with the scanner switched off, no viewfinder is composed and the list's surface starts right below the top bar, as in `scannerOffShowsTheListBelowTheStrip`.
  - Verify `./gradlew :feature:pokedex:impl:testDebugUnitTest --tests '*PokedexScreenTest*'` passes.
- [ ] 4.8 :feature:pokedex:impl — (optional, recommended) a drag test tagged `// Scenario: Sheet corners fade with the camera` on `Pixel7`: hold a partial drag on the sheet (`down`, then `moveBy` up a third of the travel, no `up`), capture the root, and check a corner pixel below the area's bottom is not `CameraColors.background`. Verify the test passes
- [ ] 4.9 :feature:pokedex:impl — goldens:
  - First run `./gradlew :feature:pokedex:impl:testDebugUnitTest -Proborazzi.test.verify=true` before recording, and note in the PR which goldens fail.
  - Expect the `phone*` camera-on goldens (Pixel 7 portrait, full width) to fail.
  - `tablet`, `foldable`, `tabletLoading` and `tabletMatch` render `PokedexContent` at full window width (1280dp, 841dp), which does not represent production (a 360dp list pane). They are wide enough that the rule fills them with `uprightPortrait = false`; check each and note it in the PR.
  - `phonePermission`, `tabletPermission`, `phoneExpanded`, `tabletExpanded`, `phoneScannerOff` and `tabletScannerOff` must stay pixel-identical (verify passes); any failure there is a bug.
  - Then run `./gradlew recordRoborazziDebug` and review every diff: only the two corner wedges may change. Look for anti-aliasing changes along the aim box's edges and brackets and along the corner arcs.
  - Commit the PNGs, including the new `tabletListPane`, `phoneLandscapePane` and `tabletPortraitPane`.
  - Verify `./gradlew :feature:pokedex:impl:testDebugUnitTest -Proborazzi.test.verify=true` passes.

## 5. Verification on a device (:app, no code change)

- [ ] 5.1 :app — install the debug build on emulators with `android install` and `android run`, capture with `android screen capture`, and compare against a build of `main` where noted:
  - **Phone in portrait:** the camera shows behind both corners. No camera shows above the scanner area (the top strip at `-r` is drawn by the EXTERNAL-mode `SurfaceView`, so check its clipping). Dragging the sheet shows no black flash at the corners. The camera starts once (no restart).
  - **Phone in landscape (360dp list pane) and a landscape tablet or unfolded foldable list pane:** compared with `main`, the framing of a fixed scene is the same (no zoom) and the corners are the same dark background.
  - **Portrait tablet or foldable list pane:** the camera shows behind the corners, with framing the same as `main`. No camera shows above the scanner area: here the `-r` strip is on screen under the status bar, so this is where the SurfaceView's clipping really matters.
  - **Compact multi-window on a landscape tablet display:** framing the same as `main`, corners dark.
  - **Cold-start cost:** measure with the existing `:benchmarks` startup macrobenchmark (or a Perfetto trace) on an emulator, before and after, and note the difference.
  - Check `./gradlew :app:testDebugUnitTest` still passes.

## 6. Gate

- [ ] 6.1 Run `./gradlew spotlessApply && ./gradlew spotlessCheck lint coverageVerify assembleDebug -Proborazzi.test.verify=true` and verify it passes

## Workflow follow-up

- Open the PR with `Fixes #31` in its body and the ticked tasks.md.
- Ask the `android-reviewer` agent to audit the diff before committing, and the `android-qa` agent to run the scenarios on a phone (portrait and landscape) and a tablet (portrait and landscape) after `/opsx:apply`.
- Archive the change with `/opsx:archive` in its own `docs(openspec)` commit or PR after the implementation is merged.
