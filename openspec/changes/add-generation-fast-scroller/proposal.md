# Proposal

Issue: #41

## Why

The home list holds all 1,025 species in Pokédex order. Reaching "somewhere in the Sinnoh dex" or "the start of Paldea"
without typing a name means flinging through hundreds of rows. Players think of the Pokédex in generations. A fast-scroll
thumb that steps by generation makes the whole list quick to browse. It shows only while the list moves, so it never
takes width from the rows.

In the collapsed sheet under the scanner, an upward swipe expands the sheet before the list moves (Material's sheet
takes upward drags first, and that stays as it is). So the visual thumb shows there only when an already-scrolled list
is scrolled back. Its main use is in the expanded sheet, with the scanner off and in the list pane. The TalkBack control
works in every sheet position. The maintainer accepted this gap: no thumb on a sheet drag and no grab area.

This extends the MVP's "search-first home: Pokédex-order list" (docs/MVP_SPEC.md, Must-have). It adds no new product
area, so MVP_SPEC.md needs no scope change.

## What Changes

- While the full list in Pokédex order is scrolled, a thin fast-scroll thumb fades in on the list's end edge (right in
  left-to-right languages, left in right-to-left). It fades out about 1.5 seconds after scrolling and dragging stop.
  While the list is still, nothing is drawn and the rows keep their full width.
- Dragging the thumb snaps it generation by generation and jumps the list to each generation's first species (1 →
  #0001, 4 → #0387, 9 → #0906).
  - While the finger is down, a bubble on the list side of the finger reads "Generation N · Region" and tick marks
    labelled 1 to 9 show along the track.
  - The bubble and the ticks go when the finger lifts.
- Scrolling by hand keeps the thumb on the generation at the top of the list.
- There is no thumb over typed results, a scan match, the loading skeleton, an error or the empty state. It works again
  when the full list returns.
- The thumb spans only the visible list area in every home layout: the collapsed sheet, the expanded sheet, the
  scanner switched off, and the list pane beside an entry. Dragging it never moves the sheet or closes an entry shown
  beside the list.
- TalkBack reaches a generation control after the search field and before the first row. It is announced as an
  adjustable slider with the current generation ("Generation 1, Kanto"). Swiping up or down moves one generation, jumps
  the list and announces the new generation. The control is there for TalkBack even while the visual thumb is hidden.
- Each generation's first species comes from the bundled data (the generation each species already stores), not from
  number ranges in the code. The list gets those boundaries through `:core:domain`.

## Non-goals

Copied from issue #41's Out of scope:

- An always-visible generation index along the edge (decided against: it doesn't fit the collapsed sheet and takes
  width from every row)
- Filtering the list by generation (chips, tabs or a setting), and generation section headers or generation labels
  inside the rows
- A thumb over search results or a scan match, since their order is best match, not Pokédex order
- Position-based scrolling inside a generation, and haptic ticks while dragging
- Changes to the entry screen, the scanner area, the sheet's positions, the search field or search behaviour
- A new module, a new library or a `:core:database` schema change. The generation is already stored per species.
- Translating region names. The app ships in English only.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `pokedex-browse`: gains two requirements. One covers jumping through the full list by generation with the
  fast-scroll thumb: it appears while scrolling, snaps and jumps when dragged, follows hand scrolling, and is absent for
  search, a match, loading, an error or no results. The other covers the generation control TalkBack uses.
- `home-screen`: gains a requirement that the thumb works in every home layout: collapsed and expanded sheet, scanner
  off, and the list pane beside an entry. It spans only the visible list, leaves the sheet where it is and leaves an
  entry open beside the list.

## Impact

- `:core:model`: a new `GenerationStart` value (generation number, first species' id).
- `:core:database`: a new read-only `SpeciesDao` query over the existing `species.generation` column, and a row type.
  The schema and the bundled database are unchanged.
- `:core:domain`: `SpeciesRepository.generationStarts()` and a new `GetGenerationStartsUseCase`.
- `:core:data`: `OfflineSpeciesRepository` implements it behind `catchStorage`.
- `:core:testing`: `FakeSpeciesRepository` gains the seeded boundaries, and a national-dex fixture is added.
- `:feature:pokedex:impl`:
  - `PokedexState` and `PokedexViewModel` load the boundaries and know when the full list is showing.
  - `PokedexScreen.kt`'s `SpeciesList` hosts a new `GenerationScroller` composable (thumb, bubble, ticks and the
    TalkBack slider) and owns the list's scroll state.
  - New pure helpers map list positions to generations.
  - New `pokedex_`-prefixed strings for the nine regions, the bubble and the spoken state.
  - New tests and goldens.
- No new dependency, no ADR, no `:feature:*:api` or `:app` change, and no other form factor. The thumb is built from
  Compose foundation and Material 3 APIs the module already uses. Android's docs list no stock fast scroller for
  Compose lazy lists.
