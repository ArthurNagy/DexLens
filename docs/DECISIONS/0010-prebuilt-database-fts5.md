# 10. Prebuilt Room database with FTS5 on the bundled SQLite driver

Date: 2026-11-09

## Status

Accepted

## Context

All Pokédex data ships in the APK and must be searchable by prefix, ignoring case, accents and punctuation, in every
language. Room 3 offers `@Fts4` and `@Fts5`. It was unknown whether `createFromAsset`, `@Fts5` and `BundledSQLiteDriver`
work together, and whether DAO tests could run FTS5 at all.

A throwaway spike on a Pixel 6 running the Android 17 beta built a file from Room's exported schema with Python's
sqlite3, then opened it through `createFromAsset`:

- `createFromAsset` + `@Fts5` + `BundledSQLiteDriver`: passed. Validation succeeded on first and second open, and prefix
  `MATCH` queries returned the expected rows (bundled SQLite 3.50.1).
- `AndroidSQLiteDriver` on the same device: `no such module: fts5` (platform SQLite 3.50.6).
- Robolectric's framework driver: `no such module: fts5`, while FTS3 and FTS4 are available.

Alternatives considered: `@Fts4`, which runs on the framework driver but has weaker prefix indexing and no benefit on
the bundled driver; a `LIKE` scan over a normalized column, which cannot do token prefixes efficiently.

## Decision

Use `@Fts5` with `BundledSQLiteDriver` and `createFromAsset`. Generate the prebuilt file from Room's exported schema.
Local tests replace the Android build of the bundled driver with its host-JVM build, because FTS5 is absent from the
framework driver that android-forge's Room convention normally uses in tests.

## Consequences

- The app always runs a known SQLite version with FTS5.
- The bundled native library adds a few megabytes per ABI to the APK.
- DAO tests depend on the host-JVM native library. They run on Apple Silicon macOS and Linux, the platforms that library ships for.
