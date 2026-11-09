## Context

`:core:database` holds every fact the screens need. This change adds the domain model, repositories, two features and
the app's adaptive navigation.

## Goals / Non-Goals

**Goals:** a fast search-first home, a complete entry, the list-detail layout, polished states and accessibility.

**Non-Goals:** camera, downloads, learnsets.

## Decisions

- **Form factors:** `:app` on phone, tablet and foldable. The home is the Nav 3 list pane and the entry the detail pane.
  Compact windows show one pane at a time. Expanded windows show both.
- **Modules:** entities in `:core:model`; `SpeciesRepository`, `TypeChartRepository`, use cases, the name normalizer and
  matchup calculation in `:core:domain`; `OfflineSpeciesRepository` and `OfflineTypeChartRepository` in `:core:data`.
  Screens in `:feature:pokedex:impl` and `:feature:entry:impl`.
- **In-memory lists.** 1,025 rows are small, so the repository loads the summaries once and keeps them, with no paging.
- **Search.** Queries are normalized like the index, turned into a quoted FTS5 prefix query, and hits are ranked in the
  use case: exact name, then name prefix, then token prefix, with Pokédex order breaking ties. Digits or `#digits` search
  by number. A padded number such as `#025` is exact, and an unpadded one also includes numbers starting with it.
- **Latest query wins.** The ViewModel collects queries with `collectLatest`, so a slow search cannot overwrite a newer one.
- **Display language.** Species names and genus follow the device language when bundled, otherwise English. Everything
  else is English like the UI. The device language is read through an interface so tests stay deterministic.
- **Matchups.** A pure function multiplies the chart's factors for the defender's types.
- **Evolution text.** The database stores structured conditions, and the entry formats them from string resources.
- **Art slot.** Both screens render through one `SpeciesArt` composable taking a nullable image path. Null draws the
  type-tinted placeholder, which keeps goldens deterministic.
- **Replacing details.** A new `Navigator.navigateDetail` replaces a detail on top instead of stacking it, so Back on a
  wide window returns to the list.
- **Shared element.** `SharedTransitionLayout` around `NavDisplay`. The art of the tapped row is the shared element.

## Risks / Trade-offs

- Normalization exists in Python and Kotlin → both carry the tricky names as tests against the real index.
- Mixed-language screens for non-English users → accepted for v1. UI strings can be translated later.

## Migration Plan

None.

## Open Questions

None.
