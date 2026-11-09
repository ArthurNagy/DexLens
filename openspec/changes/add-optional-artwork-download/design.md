## Context

The sprites repository holds 1,025 official artworks totalling about 127 MB and 1,025 small sprites totalling about 1 MB.
The flavor text CSV is about 9 MB. A full run on a Pixel 6 transferred about 143 MB and stored about 25 MB: 18 MB of
384-pixel WebP art, 4 MB of sprites and 3 MB of flavor text.

## Goals / Non-Goals

**Goals:** resumable, polite to GitHub, honest about size, and a complete app without it.

**Non-Goals:** re-downloading updates, bundling content, background refresh.

## Decisions

- **Form factors:** `:app` on phone, tablet and foldable. The offer is a dialog scene and settings is a normal entry.
- **Modules:** download logic and `@HiltWorker` live in `:core:data`, HTTP in `:core:network`, the writable database in
  `:core:database`, and screens in `:feature:settings:impl`. Domain interfaces in `:core:domain`.
- **WorkManager.** A unique, network-constrained `CoroutineWorker`, promoted to a foreground `dataSync` service with a
  progress notification. It survives leaving the screen and restarts itself after failure.
- **Resumable by design.** Each file is written to a temporary path and renamed when complete, and existing final files
  are skipped. Nothing is ever partly visible. Flavor text is imported in one transaction after the whole file has been read.
- **Concurrency.** A semaphore of 6 parallel fetches over one OkHttp client.
- **Host allowlist.** An OkHttp interceptor rejects any host other than `raw.githubusercontent.com`.
- **Conversion on the device.** Official art is decoded, scaled to at most 384 pixels and encoded as lossy WebP at
  quality 80. Sprites are encoded the same way at their original size.
- **Separate writable database.** `ContentDatabase` holds flavor text so the prebuilt database can be replaced freely.
  It stores English plus the display language, to keep size down.
- **Flavor text cleanup.** Soft hyphens, form feeds and line breaks become spaces at import.
- **Coil reads local files only.** The Coil network artifact is not used, so art can never be loaded from the network.
- **Backups off.** `allowBackup` is false, so downloaded content and databases never leave the device through Auto Backup.

## Risks / Trade-offs

- Downloading 127 MB on a metered network is costly → the size is shown first and the user chooses to start it.
- Foreground service limits on Android 14 and later → the service starts while the app is visible and the work is resumable.
- Raw GitHub rate limits → limited concurrency and automatic retry through WorkManager.

## Migration Plan

None.

## Open Questions

None.
