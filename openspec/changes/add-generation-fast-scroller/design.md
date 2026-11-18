# Design

## Context

See proposal.md (Why) for the motivation and the specs for the behaviour. What the code looks like today:

- **List data.** `PokedexViewModel` fills `PokedexState.results` from `SearchSpeciesUseCase`; a blank query returns
  `SpeciesRepository.summaries()`, every species in Pokédex order. `SpeciesSummary` (`:core:model`) has id, name and
  types only. `results` answer `query`, or the query before it until the new answer arrives, so a just-cleared query can
  still show old search results for a moment. `PokedexState.listed` is the match or `results`.
- **Generation data.** `SpeciesEntity.generation` (`:core:database`) is already in the prebuilt database. Today it
  reaches only `SpeciesDetail.generation`, through `OfflineSpeciesRepository.loadDetail`. The bundled data has nine
  generations, each a contiguous, increasing run of ids: first ids 1, 152, 252, 387, 494, 650, 722, 810 and 906
  (checked with `SELECT generation, MIN(id) … GROUP BY generation`, and no id is followed by an id of a lower
  generation).
- **The list UI.** `PokedexScreen.kt`: `PokedexContent` → `ScannerHome` (a `BottomSheetScaffold`) or `ListOnlyHome` →
  `SearchAndList` (a `Column` of `SearchField` and `PokedexBody`) → `SpeciesList`, a `LazyColumn` with no hoisted state.
  - With the sheet collapsed, the sheet content is still sized `windowHeight - stripHeight - dragHandle`, so the
    bottom of the `LazyColumn` lies below the window.
  - On a wide window home is a 360dp list pane beside the entry (`canNavigateBack = true`).
  - The sheet moves by its own anchored drag and by nested scroll from the list.
  - Material's `ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection` gives every upward user drag to the sheet
    first, and `sheetSwipeEnabled` is left on. So in the collapsed sheet an upward swipe expands the sheet before the
    list moves.
  - A downward drag on a scrolled list scrolls the list to its top before the sheet collapses. The list keeps its
    position only when the sheet is collapsed another way: Back, or dragging the search field or handle.
  - So in the collapsed sheet the list's position changes, and the thumb appears, only when an already-scrolled list is
    swiped down. This change keeps the sheet's behaviour (Out of scope: "the sheet's positions"). The home-screen spec
    states that path, and the TalkBack control works in every sheet position. See decision 8.
- **Stack.** Compose BOM 2026.09.00 with Material 3. `android docs search "fast scroller compose lazy list"` (and
  "scrollbar compose foundation") found no stock fast scroller or scrollbar for Compose lazy lists on Android. The only
  scrollbar is Compose Desktop's. So this is a small custom composable.

## Goals / Non-Goals

**Goals:**

- Take the generation boundaries from the bundled database and pass them through `:core:domain`. The feature never
  touches `:core:data` and never hard-codes number ranges.
- One `GenerationScroller` composable used by `SpeciesList`, so every home layout gets it unchanged.
- Keep geometry and position mapping in pure functions that unit tests cover, so the composable stays thin and JaCoCo's
  85% holds.
- Leave all existing goldens pixel-identical: nothing draws while the list is still.

**Non-Goals:**

- No proportional (position-in-generation) scrolling, no haptics, no change to the sheet, the scanner area, the search
  field or search (issue #41, Out of scope).
- No new field on `SpeciesSummary`. See decision 1.

## Decisions

### 1. Generation boundaries: a dedicated query, not a field on every summary

- **`:core:model`.** Add `data class GenerationStart(val generation: Int, val firstSpecies: SpeciesId)` to `Species.kt`,
  with no defaults.
- **`:core:database`.** Add
  `SpeciesDao.generationStarts(): List<GenerationStartRow>`, which runs
  `SELECT generation, MIN(id) AS first_species_id FROM species GROUP BY generation ORDER BY generation`.
  `GenerationStartRow(generation, @ColumnInfo("first_species_id") firstSpeciesId)` goes in `PokedexRows.kt`. It is a
  read query over an existing column: no schema change, no new database version, no change to `dexlens.db`.
- **`:core:domain`.** Add `SpeciesRepository.generationStarts(): Result<List<GenerationStart>, DomainError>` (KDoc:
  "each generation's first species, in generation order, from the bundled data"). Add
  `GetGenerationStartsUseCase` (public, `@Inject`, returns the repository's result). It belongs to the species
  repository because it is a fact about species: one repository per domain type.
- **`:core:data`.** `OfflineSpeciesRepository.generationStarts()` maps the rows inside `catchStorage { }`. There is no
  cache: the ViewModel calls it once, and it is nine rows.

**Alternative considered:** add `generation: Int` to `SpeciesSummary` and work out the boundaries in the UI. That changes
the shared list entity for one screen, touches every summary builder and fixture, and adds a column to the hottest
query, which the camera matcher's `speciesById` also uses. The boundary query keeps `SpeciesSummary` as it is and gives
the UI exactly the nine numbers it needs.

**Mapping a row to a generation.** A list row's generation is the last `GenerationStart` whose `firstSpecies` id is at
most the row's id. This depends on generations being contiguous runs of ids. That is true of the data (see Context),
and the `:core:database` test pins it, so a data refresh that broke it would fail the build instead of mis-labelling
rows.

### 2. State: boundaries plus "the full list is showing"

`PokedexState` gains two fields, both set in `Initial`:

- `generations: List<GenerationStart>` (empty).
- `answeredQuery: String?` (null), the query that `results` currently answer. `search()` sets it in the same
  `reduce` as `results`, on success and on error.

It also gains one derived property:

- `fullListShown: Boolean` = `!loading && error == null && !matchListed && query.isBlank() && answeredQuery?.isBlank() == true && results.isNotEmpty()`.

`answeredQuery` is what keeps the thumb off the stale results of a just-cleared query. Without it, `query` reads blank
while `results` still hold "pika"'s answer. A digit query such as "1" even returns #0001, #0152 and later starts in
increasing order, so "the boundaries are found in the list" is not a safe test either.

`PokedexViewModel` gains `GetGenerationStartsUseCase` and a `loadGenerations()` intent started in the container's
`onCreate`, next to `loadMatcher()`.
- On success it reduces `generations`.
- On failure it logs `Timber.w` and leaves the list empty. The list still works, just without the thumb or the control.
  This is deliberately not a user-visible error: the list itself loaded.

### 3. From boundaries to list positions (pure, in `:feature:pokedex:impl`)

There is a new file `GenerationIndex.kt` (internal). It holds:

- `data class GenerationSlot(val generation: Int, val firstIndex: Int)`.
- `generationSlots(listed: List<SpeciesSearchResult>, starts: List<GenerationStart>): List<GenerationSlot>`. For each
  start whose species is in `listed`, it gives that species' index. Starts that are not found are dropped. The result is
  empty when fewer than two slots remain, because a one-slot thumb is pointless.
- `slotOfIndex(index: Int, slots: List<GenerationSlot>): Int`, the last slot whose `firstIndex` is at most `index`
  (a binary search). It gives the generation at the top of the list.
- `slotAt(y: Float, trackTop: Float, trackHeight: Float, count: Int): Int`, which clamps to `0 until count`.
- `slotCentre(slot: Int, trackTop: Float, trackHeight: Float, count: Int): Float`.

The track is split into `count` equal slots, one per generation, whatever its size. This is generation-stepped, not
proportional to the species count, as the issue asks (no position-based scrolling inside a generation).

`PokedexContent` adds `slots` to `ListModel`. It is computed in the same `remember` as the rest of the model, with
`state.generations` and `state.fullListShown` added to its keys, as
`if (state.fullListShown) generationSlots(state.listed, state.generations) else emptyList()`. `PokedexBody` passes it to
`SpeciesList`, which shows the scroller only when `slots` is not empty. So search, a match, loading, an error or no
results give no thumb and no TalkBack control.

### 4. The `GenerationScroller` composable

There is a new file `GenerationScroller.kt` (internal).
`SpeciesList` hoists `val listState = rememberLazyListState()` and becomes
`Box(modifier.semantics { isTraversalGroup = true }) { LazyColumn(state = listState, …); if (slots.isNotEmpty()) GenerationScroller(listState, slots, Modifier.matchParentSize()) }`.
The `LazyColumn` keeps its tag, content padding and rows. The bottom inset and IME modifier stay on the outer `Box`.

**Track geometry: the visible list only.** The scroller reads its own window bounds with `onGloballyPositioned`
(`boundsInWindow()`, which is clipped to the window and to its ancestors).
- The track runs from the list's top plus `AppSpacing.xs` to the visible bottom, minus the navigation-bar inset and
  `AppSpacing.md`, matching the list's content padding.
- With the sheet collapsed, the part of the list below the window is excluded, so the ticks never run off-screen.
- The measured values live in a `MutableState` read only inside `offset { }` lambdas and draw. Moving the sheet then
  re-places the thumb without recomposing the rows.

**Thumb.** The thumb is a 48dp × 48dp touch target, placed with `Modifier.offset { }` so it is centred vertically on
its slot and aligned to the end edge with `Alignment.TopEnd`, which mirrors in right-to-left layouts. Only a thin
visual pill is drawn: 4dp × 32dp, `MaterialTheme.colorScheme.primary`, rounded, 4dp from the end edge.
- While dragging, the pill sits on the dragged slot.
- Otherwise it sits on `slotOfIndex(listState.firstVisibleItemIndex)`, read in a `derivedStateOf`, so hand scrolling
  moves it slot by slot.

**Visibility and fade.** A `visible` state starts false.
- A `LaunchedEffect(slots)` collects
  `snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }.drop(1)` with
  `collectLatest`. Each change sets `visible = true`, waits `FADE_DELAY = 1_500.ms`, then hides the thumb unless a drag
  is in progress.
- The end of a drag starts the same delay.
- Alpha animates with `animateFloatAsState` (fade in 150ms, out 300ms).
- The effect triggers on a change of scroll position, not on `isScrollInProgress`. So a drag on the collapsed list that
  only moves the sheet (nested scroll consumes it) does not flash the thumb.
- Keying on `slots` drops the first emission after the full list returns, so coming back from a search does not flash
  it either.

**Gesture and the sheet.** The touch target's `pointerInput` exists only while `visible` is true.
- When hidden, the node has no pointer-input modifier at all. Compose doesn't pass pointer events to siblings, so a
  hidden target would otherwise swallow taps on the rows under the end edge.
- The gesture is `awaitEachGesture { val down = awaitFirstDown(); down.consume(); drag(down.id) { it.consume(); … } }`.
  It consumes the down and every move, with no touch slop.
- The sheet's anchored drag and the `LazyColumn`'s scrollable are an ancestor and a sibling below. They see consumed
  changes and never start, so the sheet stays where it is.
- The finger's position is kept in track space, not in the touch target's local space. The target snaps from slot to
  slot, so its local pointer coordinates shift on every snap and would make the slot jitter.
  - At the down event, `fingerY = slotCentre(currentSlot) + (down.position.y - targetSize / 2)`.
  - Each move adds `change.positionChange().y` to `fingerY`.
  - The slot is `slotAt(fingerY, trackTop, trackHeight, count)`.
- The jump is `listState.scrollToItem(slots[slot].firstIndex)`, launched in the composable's scope whenever that slot
  changes. `scrollToItem` cancels a running fling (same mutator priority) and does not go through
  nested scroll, so it cannot drag the sheet either.
- The scroller never calls `onOpenSpecies` and does not handle Back, so an entry open beside the list is untouched.

**Bubble and ticks.** They are composed only while a drag is in progress.
- **Ticks:** for each slot, a 4dp dot and a `labelSmall` label (`generation.toString()`, so always the digits 1–9) at
  `slotCentre`, in a column along the track just inside the thumb.
- **Bubble:** a `Surface` (`secondaryContainer`, `shapes.medium`, `labelLarge`) on the start side of the thumb, which is
  the list side in both directions. It is vertically centred on the thumb, clamped inside the track, and reads
  `pokedex_generation_bubble` ("Generation %1$d · %2$s").
- If a generation has no region string (a future data set with a 10th generation), the bubble falls back to
  `pokedex_generation_number` ("Generation %1$d"). The ticks and the slider still work.
- The bubble, ticks and pill have no semantics of their own (see decision 5).
- Test tags: `pokedex:fastScroll:thumb`, `pokedex:fastScroll:bubble`, `pokedex:fastScroll:tick:<generation>`.

### 5. Accessibility: one always-present slider node

**What is composed when.**
- The control node (`pokedex:fastScroll:control`, the 48dp target) is always composed while `slots` is not empty.
- The pill (`pokedex:fastScroll:thumb`) is composed only while `visible` is true or its fade-out is still running. It
  is removed from composition once alpha reaches 0, so "no thumb displayed" means the node does not exist.
- The bubble and ticks are composed only while a drag is in progress.

The control node carries the semantics, always, even while `visible` is false. Semantics take no part in pointer hit
testing, so it blocks no touches. TalkBack's explore-by-touch does find nodes by their bounds, though. Touching the
48dp square at the end of the row under the current slot focuses the slider instead of that row. That is an accepted
trade-off: the square is one row's end edge, the row itself is still reachable by swiping and by touching anywhere else
on it, and it is what lets a TalkBack user find the control by touch. Task 8.1 checks it. The node is tagged
`pokedex:fastScroll:control`:

- `progressBarRangeInfo = ProgressBarRangeInfo(current = slot + 1f, range = 1f..count.toFloat(), steps = count - 2)`.
  That gives nine discrete positions for nine generations, because Compose's `steps` counts the stops between the two
  ends (Material's `Slider` uses the same convention).
- `setProgress { v -> jump to slot (v.roundToInt() - 1).coerceIn(0, count - 1); true }`.
- `stateDescription = "Generation N, Region"` (`pokedex_generation_state`, "Generation %1$d, %2$s", or the number-only
  fallback) for the generation at the top of the list.
- `traversalIndex = -1f` inside the `isTraversalGroup` box in `SpeciesList`. It is ordered before the `LazyColumn` and
  its rows. The search field comes earlier in the surrounding `Column`, so TalkBack goes search field → control →
  first row.
- No `contentDescription`. TalkBack then reads "Generation 1, Kanto, slider", which matches the issue's wording.

A node with `progressBarRangeInfo` and `setProgress` is exposed as a `SeekBar`. TalkBack's swipe up and down send
`ACTION_SCROLL_FORWARD`/`BACKWARD`, which Compose turns into `setProgress(current ± one step)`; Material's `Slider`
relies on the same path. A jump scrolls the list, the top-of-list generation changes, and the new `stateDescription` is
announced.

### 6. Strings (`:feature:pokedex:impl`, prefix `pokedex_`)

- Regions: `pokedex_region_kanto`, `_johto`, `_hoenn`, `_sinnoh`, `_unova`, `_kalos`, `_alola`, `_galar`, `_paldea`.
  `regionRes(generation: Int): Int?` in `GenerationIndex.kt` maps 1–9 to them, and anything else to null.
- `pokedex_generation_bubble`: "Generation %1$d · %2$s".
- `pokedex_generation_state`: "Generation %1$d, %2$s".
- `pokedex_generation_number`: "Generation %1$d".

The words match the entry's `entry_generation_value` ("Generation %1$d"). Region names are English only and are not
translated (Out of scope).

### 7. Form factors

Only `:app` phone, tablet and foldable change, through `:feature:pokedex:impl`. No Wear, TV, Auto, Automotive, XR or
glasses surface exists.

- **Compact window:** the full-width list, in the collapsed sheet, the expanded sheet or with the scanner off.
- **Medium and expanded windows** (phone landscape, tablets, unfolded foldables): home is the 360dp list pane, and the
  scroller sits on the pane's end edge, next to the entry pane boundary in LTR.
- The thumb is never laid out in the entry pane.
- There are no orientation or resizability locks.
- With the sheet collapsed in a short window (a phone in landscape), the track can be about 140dp tall, about 15dp per
  slot. See Risks.

### 8. The collapsed sheet keeps its behaviour (maintainer decision)

The maintainer accepted the gap. From the top of the collapsed list the visual thumb can't be brought up, because an
upward swipe expands the sheet first. There is no thumb on a sheet drag and no always-present grab area.

## Risks / Trade-offs

- [Short track in a landscape collapsed sheet: nine labels at about 15dp each crowd together] → Labels use `labelSmall`
  (16sp line height). When a slot is shorter than the label's line height, only the odd labels (1, 3, 5, 7, 9) are drawn,
  next to all nine dots. The bubble always names the exact generation. This is a visual detail, not a spec change.
  Task 6.8's goldens pin the normal case, and QA checks landscape.
- [TalkBack swipe mapping depends on Compose's accessibility delegate] → Robolectric can only invoke `SetProgress`
  directly. Tests pin the semantics (range, steps, state, the `SetProgress` action, traversal order), and the
  `android-qa` device journey checks the real TalkBack swipe and spoken order on `medium_phone`.
- [A data refresh that made generations non-contiguous would mis-label rows] → The `:core:database` test asserts the
  nine starts and that the generation never decreases with id.
- [`onGloballyPositioned` fires on every frame while the sheet is dragged] → It writes only a `MutableState` that is read
  in layout and draw lambdas, so the rows do not recompose. While the thumb is hidden, only the semantics-only control
  node is composed (decision 5).
- [A 48dp target over the rows' end edge while the thumb is visible] → It intercepts touches only within that square and
  only while visible, as standard fast scrollers do. The spec's "Still list keeps full-width rows" scenario pins the
  hidden case.
- [Robolectric partial drags for goldens] → Goldens hold the gesture open (`down`, `moveTo`, capture, then `up`), as the
  existing sheet tests do with `performTouchInput`. If a held drag does not render in Robolectric, the fallback is a
  test-only `initialDrag` slot parameter (internal, default null) on `GenerationScroller`, used only by the goldens.
- [JaCoCo 85%] → The pure helpers in `GenerationIndex.kt`, the repository, the DAO query and the use case are each
  unit-tested. The composable is exercised by screen tests (drag, hand scroll, fade timing, semantics). No production
  code is test-only.

## Migration Plan

None. This is a read-only query on the existing database, and nothing is persisted. Rollback is reverting the PR.
