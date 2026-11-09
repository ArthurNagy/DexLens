Issue: none

## Why

Every DexLens feature reads the same species data, and it must be available offline from the first launch. The data
has to be reproducible from PokeAPI, match Room's schema exactly, and contain no game art or game flavor text.

## What Changes

- Add `tools/build_database.py`, which shallow-clones PokeAPI's CSV tables into a git-ignored folder at a pinned commit
  and generates the prebuilt database from Room's exported schema.
- Add `:core:database` with the bundled database, opened through Room 3 `createFromAsset` on the bundled SQLite driver,
  and an FTS5 search index.
- Commit the generated database as an app asset.
- Validate row counts and tricky names in the script and in tests that open the real asset.

## Capabilities

### New Capabilities
- `pokedex-data`: the bundled species data and its search index, available offline from first launch.

### Modified Capabilities

## Non-goals

- Learnsets, and any move data beyond the names evolution conditions need.
- Screens and use cases over this data (later changes).
- Official art, sprites or word-for-word game flavor text.

## Impact

New module `:core:database`, new `tools/` folder, an asset of a few megabytes. `:core:data` gains a dependency on it.
