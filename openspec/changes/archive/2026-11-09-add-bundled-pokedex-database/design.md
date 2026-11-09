## Context

DexLens must work in airplane mode from the first launch. PokeAPI's CSV tables (BSD-3-Clause) are the source. A device
spike on the Pixel 6 (Android 17 beta) proved that `createFromAsset`, `@Fts5` and `BundledSQLiteDriver` work together
and that the framework driver has no FTS5 on either the device or Robolectric. See ADR 0010.

## Goals / Non-Goals

**Goals:** complete offline data, Room validation by construction, a reproducible build, tests that open the real file.

**Non-Goals:** learnsets, writing to this database at runtime, downloaded content (a later change uses its own store).

## Decisions

- **Form factors:** none. This change has no UI.
- **Module:** `:core:database` holds entities, DAOs and `DexDatabase`. Only `:core:data` depends on it.
- **Read-only prebuilt database.** `createFromAsset` with `BundledSQLiteDriver`. Downloaded flavor text will live in a
  separate database, so a data update can replace this file with destructive fallback and lose nothing.
- **Schema from Room.** The script reads `core/database/schemas/.../1.json`, runs each `createSql` and `setupQueries`,
  so tables and the identity hash always match. Alternative rejected: handwritten DDL, which drifts from the entities.
- **Pinned source.** The script fetches a specific PokeAPI commit with `git fetch --depth 1`, into git-ignored
  `tools/.cache/`. Rows are inserted in a stable order and the file is vacuumed, so two runs from one commit on one
  machine produce byte-identical files (checked with `sha256sum` when generating the asset).
- **Flat varieties.** `pokemon.csv` rows become a `variety` table with stats and types inline, so an entry needs no pivot.
- **English for secondary text.** Abilities, egg groups, forms and evolution conditions store English only, because the
  UI is English. Species names and genus keep every language.
- **Evolution rows.** Only `is_default` rows are used, falling back to the latest version group, and rows that differ
  only in conditions the entry does not show are merged. Alcremie's 63 spin rows become one row per held sweet.
- **Search index.** A standalone `@Fts5` table with one row per species and language. The `search` column holds
  normalized tokens plus a compact form, for example `mr mime mrmime`, which makes punctuation-free queries work.
  Normalization folds Latin accents only, so Japanese dakuten stay distinct.
- **Tests on the real file.** The module's Robolectric tests swap in the host JVM build of the bundled driver so FTS5 and
  the real asset are testable in CI. An instrumented test repeats the check on the device.

## Risks / Trade-offs

- PokeAPI data can have gaps → the script fails on any missing species, type, stat or ability.
- Python's SQLite is newer than the driver's → FTS5's on-disk format is stable and the device test covers it.
- Asset size grows the APK → measured and reported. Only needed columns are stored.

## Migration Plan

First version. A later data update bumps the version and uses destructive fallback to recopy the asset.

## Open Questions

None.
