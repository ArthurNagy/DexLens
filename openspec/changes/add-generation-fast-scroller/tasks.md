# Tasks

Order: `:core:model` → `:core:database` → `:core:domain` → `:core:testing` → `:core:data` → `:feature:pokedex:impl`.
There are no `:feature:*:api` or `:app` code changes; the `:app` checks are verification only. Tests use fakes from
`:core:testing` and Orbit's test harness, never mocks. Production shows home as a 360dp list pane from medium width up,
so tablet tests use `show(pane = true)` as the existing `tabletListPane` test does. Coverage is aggregated and must stay
at or above 85%. Every new production line below has a test in the same group.

## 1. Domain value (:core:model)

- [x] 1.1 :core:model — add `data class GenerationStart(val generation: Int, val firstSpecies: SpeciesId)` to
  `Species.kt`, with no defaults and KDoc saying "the first species of a generation, by National Pokédex number". Verify
  `./gradlew :core:model:compileKotlin` passes. Data class only, so it needs no test of its own; 2.2 and 3.2 cover it.

## 2. Boundary query (:core:database)

- [x] 2.1 :core:database — add
  `data class GenerationStartRow(val generation: Int, @ColumnInfo(name = "first_species_id") val firstSpeciesId: Int)`
  to `PokedexRows.kt`. Add
  `@Query("SELECT generation, MIN(id) AS first_species_id FROM species GROUP BY generation ORDER BY generation") suspend fun generationStarts(): List<GenerationStartRow>`
  to `SpeciesDao`. Do not change entities, the database version, the exported schema or `dexlens.db`. Verify
  `./gradlew :core:database:compileDebugKotlin` passes and `git status` shows no change under `schemas/` or `assets/`.
- [x] 2.2 :core:database — in `PrebuiltDatabaseTest`, add `generationStartsComeFromTheBundledData` tagged
  `// Scenario: Dragging jumps to each generation's first species`. It asserts that `dao.generationStarts()` is
  generations 1..9 with first ids 1, 152, 252, 387, 494, 650, 722, 810 and 906. It also asserts, with `scalar(...)`,
  that no species is followed by a species of a lower generation:
  `SELECT COUNT(*) FROM species a JOIN species b ON b.id = a.id + 1 WHERE b.generation < a.generation` is 0. Verify
  `./gradlew :core:database:testDebugUnitTest --tests '*PrebuiltDatabaseTest*'` passes.

## 3. Repository contract and use case (:core:domain, :core:testing)

- [x] 3.1 :core:domain — add `suspend fun generationStarts(): Result<List<GenerationStart>, DomainError>` to
  `SpeciesRepository`, with KDoc "each generation's first species, in generation order, from the bundled data". Add
  `GetGenerationStartsUseCase` in `core/domain/.../species/`: a public class with an `@Inject` constructor taking
  `SpeciesRepository`, implementing `UseCase`, whose `invoke()` returns `repository.generationStarts()`. Verify
  `./gradlew :core:domain:compileKotlin` passes.
- [x] 3.2 :core:testing — `FakeSpeciesRepository` gains `var generationStarts: List<GenerationStart> = emptyList()`. Its
  `generationStarts()` goes through `respond { }`, so `nextError` fails it like every other method. Add
  `nationalGenerationStarts(): List<GenerationStart>` to `SpeciesFixtures.kt` (the nine real starts). Add
  `nationalDexSummaries(): List<SpeciesSummary>`, 1,025 summaries `speciesSummary(id, name, NORMAL)` with names
  "Bulbasaur" (1), "Treecko" (252), "Turtwig" (387) and "Sprigatito" (906), and "Species <id>" elsewhere. In `SpeciesUseCasesTest`,
  test that `GetGenerationStartsUseCase` returns the seeded starts and passes through `nextError`. Verify
  `./gradlew :core:domain:test --tests '*SpeciesUseCasesTest*'` passes (`:core:domain` is a JVM module).

## 4. Repository implementation (:core:data)

- [x] 4.1 :core:data — implement `OfflineSpeciesRepository.generationStarts()` as
  `catchStorage { dao.generationStarts().map { GenerationStart(it.generation, SpeciesId(it.firstSpeciesId)) } }`.
  `FakeSpeciesDao` (`core/data/src/test`) gains a seedable `generationStarts` list and a way to make it throw. Add to
  `OfflineSpeciesRepositoryTest`:
  - (a) the rows map to `GenerationStart`s in order
  - (b) a DAO exception becomes `Err` of the storage `DomainError` that `catchStorage` produces, like the existing
    failure tests.

  Verify `./gradlew :core:data:testDebugUnitTest --tests '*OfflineSpeciesRepositoryTest*'` passes.

## 5. Feature state and position mapping (:feature:pokedex:impl)

- [x] 5.1 :feature:pokedex:impl — `PokedexState` gains `generations: List<GenerationStart>` and
  `answeredQuery: String?`, set in `Initial` to `emptyList()` and `null`. It also gains
  `val fullListShown: Boolean get() = !loading && error == null && !matchListed && query.isBlank() && answeredQuery?.isBlank() == true && results.isNotEmpty()`,
  with KDoc on why `answeredQuery` is needed (see design decision 2).
  - `PokedexViewModel` takes `GetGenerationStartsUseCase` as its last constructor parameter.
  - It adds a `loadGenerations()` intent to the container's start block that reduces `generations` on `Ok`, and on
    `Err` logs `Timber.w` and leaves the list empty.
  - `search()` sets `answeredQuery = query` in both `answer { }` branches.
  - Update the three `PokedexViewModel(...)` call sites in `PokedexViewModelTest` and `PokedexScreenTest`
    (lines ~67, ~273, ~924).
  - Update the exact-state expectations in `PokedexViewModelTest` that `answeredQuery` now changes. These are the
    `expectInternalState { copy(results = …) }` steps at about lines 131, 155, 321, 329 and 376, each of which now also
    sets `answeredQuery` to the answered query.
  - Make `settled()` (about line 92) also wait for `generations` when the fake seeds `generationStarts`, so the extra
    reduce can't land between later expectations. Existing tests seed none and keep their sequences.
  - Verify `./gradlew :feature:pokedex:impl:compileDebugUnitTestKotlin` passes.
- [x] 5.2 :feature:pokedex:impl — `PokedexViewModelTest` cases, with `FakeSpeciesRepository` seeded with three species and
  `generationStarts`:
  - `// Scenario: Dragging jumps to each generation's first species`: after start, `state.generations` equals the
    seeded starts.
  - `// Scenario: No thumb while loading, failed or empty`: `Initial` and a `nextError` on the search have
    `fullListShown == false`. A query with no hits has `fullListShown == false`. A failing `generationStarts` leaves
    `generations` empty, keeps `error == null`, and still lists the species.
  - `// Scenario: No thumb over search results`: after `onQueryChanged("pika")`, `fullListShown` is false.
  - `// Scenario: Thumb returns with the full list`: after `onQueryChanged("")` and before the blank answer arrives
    (hold the fake with a gate or check the intermediate state), `fullListShown` is false. Once it arrives, it is true.
  - `// Scenario: No thumb over a scan match`: with a match locked on, `fullListShown` is false.

  Verify `./gradlew :feature:pokedex:impl:testDebugUnitTest --tests '*PokedexViewModelTest*'` passes.
- [x] 5.3 :feature:pokedex:impl — new `GenerationIndex.kt` (internal) with `GenerationSlot`, `generationSlots`,
  `slotOfIndex`, `slotAt`, `slotCentre` and `regionRes(generation): Int?`, exactly as in design decision 3. Add the
  strings to `res/values/strings.xml`:
  - `pokedex_region_kanto` … `pokedex_region_paldea`
  - `pokedex_generation_bubble` ("Generation %1$d · %2$s")
  - `pokedex_generation_state` ("Generation %1$d, %2$s")
  - `pokedex_generation_number` ("Generation %1$d")

  Add a JVM `GenerationIndexTest`:
  - (a) `// Scenario: Dragging jumps to each generation's first species`: over `nationalDexSummaries()` and
    `nationalGenerationStarts()`, `generationSlots` gives nine slots with `firstIndex` 0, 151, 251, 386, 493, 649, 721,
    809 and 905. Starts missing from the list are dropped, and fewer than two slots give an empty list.
  - (b) `// Scenario: Thumb follows hand scrolling`: `slotOfIndex` gives slot 0 for index 0, 4 for 493 and 649 − 1 = 648,
    and 8 for 1024.
  - (c) `slotAt` clamps above the top and below the bottom and splits the track into equal slots. `slotCentre` is the
    middle of each slot, and `slotAt(slotCentre(i)) == i` for every slot.
  - (d) `regionRes` maps 1..9 to the nine region strings in order and gives null for 0 and 10.

  Verify `./gradlew :feature:pokedex:impl:testDebugUnitTest --tests '*GenerationIndexTest*'` passes.

## 6. The scroller (:feature:pokedex:impl)

Implementation note: `PokedexScreenTest` was already 1,160 lines, so the screen tests for 6.2–6.8 live in a sibling
`GenerationScrollerTest`. It renders `PokedexContent` with the same kind of `show(...)` helper (including `pane` and
`rtl`). Its goldens are `GenerationScrollerTest.phoneFastScroll`, `.tabletFastScroll` and `.phoneFastScrollRtl`.
`PokedexScreenTest` only gained the new use case in its two `PokedexViewModel(...)` constructions.

- [x] 6.1 :feature:pokedex:impl — new `GenerationScroller.kt` (internal), built as in design decisions 4 and 5:
  - track geometry from `boundsInWindow()`, read only in `offset { }` and draw lambdas
  - a 48dp touch target with a 4dp × 32dp pill, `Alignment.TopEnd`
  - `visible` driven by scroll-position changes with a 1,500ms hide delay that a drag holds open, and an animated alpha
  - pointer input only while visible, consuming the down and every move, and `scrollToItem` on slot change
  - bubble and ticks only while dragging, with the odd-labels-only fallback when a slot is shorter than the label's
    line height
  - slider semantics on the touch-target node, always present: `progressBarRangeInfo` with `steps = count - 2`,
    `setProgress`, `stateDescription` and `traversalIndex = -1f`
  - test tags `pokedex:fastScroll:control`, `pokedex:fastScroll:thumb`, `pokedex:fastScroll:bubble` and
    `pokedex:fastScroll:tick:<n>`

  In `PokedexScreen.kt`:
  - `ListModel` gains `slots`, computed as in design decision 3, with `state.generations` and `state.fullListShown`
    added to the `remember` keys.
  - `PokedexBody` passes `slots` to `SpeciesList`.
  - `SpeciesList` hoists `rememberLazyListState()` and wraps the `LazyColumn` in a
    `Box(modifier.semantics { isTraversalGroup = true })` that holds the scroller when `slots` is not empty.

  Add a `@Preview` in `PokedexPreviews.kt` only if the existing previews pattern needs one. Verify
  `./gradlew :feature:pokedex:impl:compileDebugKotlin` passes.
- [x] 6.2 :feature:pokedex:impl — `PokedexScreenTest` setup:
  - add a `fullList` state (`loaded.copy(results = nationalDexSummaries().map { SpeciesSearchResult(it, null) }, answeredQuery = "", generations = nationalGenerationStarts())`)
  - add a `rtl: Boolean = false` option to `show(...)` that wraps the content in
    `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)`
  - add helpers to scroll the list by hand (`onNodeWithTag("pokedex:list").performScrollToIndex(i)`), to drag the thumb
    to a slot (`performTouchInput { down(thumbCentre); moveTo(slotCentre) }`, with `up()` separate), and to read the
    top row's id from the `pokedex:row:*` nodes

  Verify the existing tests still pass unchanged.
- [x] 6.3 :feature:pokedex:impl — appearance tests (Pixel7 unless stated, `mainClock.autoAdvance = false` where timing
  matters):
  - `// Scenario: Thumb appears while scrolling`: after a hand scroll, `pokedex:fastScroll:thumb` is displayed and its
    right edge is within 4dp of `pokedex:list`'s right edge.
  - `// Scenario: Thumb on the left in right-to-left languages`: with `rtl = true`, the thumb's left edge is within 4dp
    of the list's left edge. While dragging, the bubble's left is at or past the thumb's right.
  - `// Scenario: Thumb fades after scrolling stops`: after a scroll, advance the clock 1,400ms and the thumb is still
    shown. Advance to 1,500ms plus the fade-out and no `pokedex:fastScroll:thumb`, `bubble` or `tick:*` node
    exists (they leave composition, per design decision 5), while `pokedex:fastScroll:control` still exists.
  - `// Scenario: Still list keeps full-width rows`: with `fullList` shown and not scrolled, no thumb is displayed, a
    row's bounds span the list's full width, and clicking the row at `(right - 8dp, centerY)` calls `onOpen` with its id.

  Verify they pass.
- [x] 6.4 :feature:pokedex:impl — drag tests:
  - `// Scenario: Dragging jumps to each generation's first species`: after a scroll to show the thumb, hold a drag and
    move it to slots 0, 3 and 8. After each move the top row is 1, 387 and 906, and the thumb's centre is at that slot's
    centre within 1px. Also check the track-space finger tracking from design decision 4: one held drag moved slowly
    down the whole track in small `moveBy` steps gives top rows that only ever increase, with no jump back.
  - `// Scenario: Bubble and ticks while dragging`: held on slot 3, `pokedex:fastScroll:bubble` has the text
    "Generation 4 · Sinnoh", sits on the start side of the thumb, and `tick:1`…`tick:9` are displayed with the texts
    "1"…"9".
  - `// Scenario: Lifting the finger clears the bubble`: after `up()`, no bubble or tick is displayed. The thumb is still
    shown at +1,400ms and gone after +1,500ms plus the fade.
  - `// Scenario: Thumb follows hand scrolling`: hand-scroll to index 500 (a generation 5 species at the top) and the
    thumb's centre is slot 4's centre.

  Verify they pass.
- [x] 6.5 :feature:pokedex:impl — absence tests:
  - `// Scenario: No thumb over search results`: with `fullList.copy(query = "s", answeredQuery = "s")`, a scroll shows
    no thumb and no `pokedex:fastScroll:control` node exists.
  - `// Scenario: No thumb over a scan match`: with a match listed, there is no control and no thumb.
  - `// Scenario: No thumb while loading, failed or empty`: with the loading, error and empty-results states, there is no
    control node.
  - `// Scenario: Thumb returns with the full list`: with `currentState` switched from a search state to `fullList`, a
    scroll shows the thumb.

  Verify they pass.
- [x] 6.6 :feature:pokedex:impl — accessibility tests:
  - `// Scenario: TalkBack reaches the generation control`:
    - With `fullList` at the top, `pokedex:fastScroll:control` exists with
      `ProgressBarRangeInfo(1f, 1f..9f, steps = 7)`, the `SetProgress` action and `StateDescription`
      "Generation 1, Kanto".
    - It has `TraversalIndex` −1 and its parent sets `IsTraversalGroup`, and that parent is the list box below the
      search field.
    - Also assert TalkBack's order through the `AccessibilityNodeInfo` of the `AndroidComposeView`
      (`traversalBefore`/`traversalAfter`, via `ViewCompat.getAccessibilityDelegate` or
      `view.createAccessibilityNodeInfo()`). If Robolectric can't produce it, note that and rely on 8.1's device check.
    - Result: Robolectric produces the control's `AccessibilityNodeInfo` as an `android.widget.SeekBar`. It doesn't
      populate the traversal-before extras, even when they are requested. So the test pins `TraversalIndex` −1, the
      `IsTraversalGroup` parent and the search field above it, and the spoken order is left to 8.1's device check.
  - `// Scenario: Adjusting the control jumps a generation`: `performSemanticsAction(SetProgress) { it(2f) }`, then 3,
    then 4. After each, the top row is 152, 252 and 387, and finally the state description is "Generation 4, Sinnoh".
  - `// Scenario: Adjusting the control down a generation`: from generation 4, `SetProgress` to 3. The top row is 252
    and the state description is "Generation 3, Hoenn". Also check that `SetProgress` with 0.4f and with 12f clamps to
    generations 1 and 9.
  - `// Scenario: Control available while the thumb is hidden`: with no scroll, no thumb is displayed and the control
    node exists with "Generation 1, Kanto".
  - `// Scenario: Control follows the list`: hand-scroll to index 730 and the control's state description is
    "Generation 7, Alola".

  Verify they pass.
- [x] 6.7 :feature:pokedex:impl — layout tests in `PokedexScreenTest`:
  - `// Scenario: Thumb in the collapsed sheet` (Pixel7 and `w914dp-h411dp-xxhdpi` pane), reached the way a user
    reaches it:
    - `expandSheet()`, then `performScrollToIndex(700)`, then `pressBack()` to collapse with the position kept. Assert
      the sheet is collapsed and no thumb is displayed.
    - Then do a real `performTouchInput { swipeDown(...) }` on `pokedex:list` over a short distance that scrolls the list
      without reaching its top, and assert the thumb is displayed.
    - Record `pokedex:search`'s bounds, then drag the thumb to slot 8.
    - `tick:9`'s bottom is above the root's bottom minus the navigation-bar inset.
    - The top row is 906.
    - `pokedex:search`'s bounds are unchanged after `up()`.
  - `// Scenario: Swiping up in the collapsed sheet expands it first`: with `fullList` at the top and the sheet
    collapsed, do a `swipeUp` on `pokedex:list` shorter than the collapsed-to-expanded distance. Afterwards the sheet is
    expanded (search field moved up, as in `expandSheet()`), the list's first visible item is still 0, and no
    `pokedex:fastScroll:thumb` node exists.
  - `// Scenario: Generation control in the collapsed sheet`: with the sheet collapsed at the top, `SetProgress` to 9.
    The top row is 906 and `pokedex:search`'s bounds are unchanged.
  - `// Scenario: Thumb in the expanded sheet`: `expandSheet()` with no text, then drag to slot 3. `tick:1`'s top is
    below `pokedex:search`'s bottom, the top row is 387, and the search field's bounds are unchanged.
  - `// Scenario: Thumb with the scanner off`: `fullList.copy(scannerEnabled = false)`, then drag to slot 3. The track
    is below the search field and the top row is 387.
  - `// Scenario: Thumb beside an open entry` (MediumTablet, `pane = true`, `canNavigateBack = true`, `onOpen` recording
    calls): drag to slot 3. The thumb and ticks lie within the 360dp pane, the top row is 387, `onOpen` was never
    called, and no Back was dispatched (the home Back handler is disabled with `canNavigateBack`, as today).

  Verify `./gradlew :feature:pokedex:impl:testDebugUnitTest --tests '*PokedexScreenTest*'` passes.
- [x] 6.8 :feature:pokedex:impl — goldens, captured while a drag is held on slot 3 so the bubble and ticks show (`down`,
  `moveTo`, `captureRoboImage()`, then `up()`):
  - `phoneFastScroll` (Pixel7, scanner on, sheet collapsed)
  - `tabletFastScroll` (MediumTablet, `pane = true`, scanner on, sheet collapsed)
  - `phoneFastScrollRtl` (Pixel7, `rtl = true`, scanner on, sheet collapsed)

  The collapsed-sheet goldens reach the thumb the same way as 6.7's collapsed-sheet test: expand, scroll, Back, then a
  short `swipeDown`. Then they hold the drag.
  Held drags render under Robolectric, so the `initialDrag` fallback wasn't needed.
  If a held drag doesn't render under Robolectric, use the fallback in design.md (an internal `initialDrag` parameter on
  `GenerationScroller`, default null) for these three goldens only, and note it in the PR.
  - First run `./gradlew :feature:pokedex:impl:testDebugUnitTest -Proborazzi.test.verify=true` with the new goldens
    ignored. Every existing golden must stay pixel-identical, because nothing draws while the list is still; any diff
    is a bug.
  - Result:
    - `recordRoborazziDebug` re-encoded `PokedexScreenTest.phoneExpanded`, `.phoneScannerOff` and `.tabletScannerOff`
      with sub-tolerance anti-aliasing deltas in the row text: at most 4/255 on a channel, in 28 to 485 pixels.
    - Re-recording those three with `SpeciesList` put back to its original structure gave byte-identical files, so the
      drift comes from this host's recording, not from this change.
    - The committed originals were kept, and they pass verify unchanged. Only the three new PNGs are added.
  - Then run `./gradlew recordRoborazziDebug`, review the three new PNGs (thumb on the end edge, bubble on the list side
    reading "Generation 4 · Sinnoh", ticks 1–9 within the visible list), and commit them.

  Verify `./gradlew :feature:pokedex:impl:testDebugUnitTest -Proborazzi.test.verify=true` passes.

## 7. Coverage

- [x] 7.1 Run `./gradlew coverageReport coverageVerify`. Check the report for `GenerationScroller.kt`,
  `GenerationIndex.kt`, `PokedexViewModel` and `OfflineSpeciesRepository`. Any uncovered branch (for example the
  number-only bubble fallback, or a clamp) gets a test in the matching group above rather than an exclusion. Verify
  `coverageVerify` passes at 85% or above.
  - Result: aggregated line coverage is 90.6%. `GenerationIndex.kt` 29/29 lines, `GenerationScroller.kt` 155/157 after
    adding a test for the number-only label (the two misses are the synthetic default-parameter lines), `PokedexViewModel.kt` 83/83, `PokedexState.kt` 27/27,
    `GetGenerationStartsUseCase.kt` 4/4, `OfflineSpeciesRepository.kt` 44/44.

## 8. Device check (:app, no code change)

- [ ] 8.1 :app — install the debug build with `android install` / `android run` on `medium_phone` (camera on) and
  `pixel_10_pro_fold`, not `medium_tablet` (its LiteRT crash with the camera on this host). Capture with
  `android screen capture`:
  - **Expanded sheet, phone portrait:** scroll and see the thumb fade in on the right and out about 1.5s later. Drag it
    to 4 and 9, and see the bubble, the ticks and the jumps. The sheet does not move and the track stays below the search
    field.
  - **Collapsed sheet, phone portrait:**
    - Expand, scroll to about generation 6, press Back to collapse, then swipe down a little so the thumb appears.
    - Drag it to 9: the list jumps, the ticks stay within the visible list, and the sheet stays collapsed.
    - From the top of a collapsed list, a swipe up expands the sheet as before.
  - **Scanner off:** the same drag works, and the track stays below the search field.
  - **Unfolded foldable list pane with an entry open:** dragging jumps the list and the entry stays.
  - **Phone landscape collapsed sheet:** check the short track's labels (odd-only fallback if crowded).
  - **TalkBack** (`medium_phone`): from the search field, swipe right. The next stop is "Generation 1, Kanto, slider"
    before the first row.
    - Swipe up three times and hear "Generation 4, Sinnoh" with Turtwig at the top. Swipe down once and hear
      "Generation 3, Hoenn".
    - Repeat with the sheet collapsed: it jumps and the sheet stays.
    - Touch-explore the end edge of the row at the current slot and note that the slider takes focus there (the accepted
      trade-off in design decision 5), and that touching the rest of the row focuses the row.
  - **RTL** (developer option "Force RTL layout direction" or a pseudo-locale): the thumb is on the left and the bubble
    on its right.

  Note the results under this task.

  Not done in this PR. The maintainer runs device QA separately (the `android-qa` agent on `medium_phone` and the
  foldable), so this task stays open.

## 9. Gate

- [x] 9.1 Run `./gradlew spotlessApply && ./gradlew spotlessCheck lint coverageVerify assembleDebug -Proborazzi.test.verify=true`
  and verify it passes.

## Workflow follow-up

- Ask the `android-reviewer` agent to audit the diff before committing, and the `android-qa` agent to run the scenarios
  on `medium_phone` and the foldable after `/opsx:apply`.
- Open the PR with `Fixes #41` in its body and the ticked tasks.md.
- Archive the change with `/opsx:archive` in its own `docs(openspec)` commit or PR after the implementation is merged.
