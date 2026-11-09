Issue: none

## Why

Bundling official art or game flavor text is not allowed. People who want them can fetch them once from PokeAPI's public
repositories, and the rest of the app must remain complete without them.

## What Changes

- A first-launch offer of a one-time optional download, showing its size and progress, skippable and available later.
- A simple settings screen with the download, theme choice and credits.
- A resumable download with limited concurrency: artwork stored as 384-pixel WebP, sprites, and flavor text.
- Downloaded art and flavor text appear in the list and entry. They keep working offline afterwards.
- Polished type-tinted placeholders remain whenever art is absent.

## Capabilities

### New Capabilities
- `artwork-download`: the optional download, its storage and its use by the app.
- `app-settings`: the settings screen.

### Modified Capabilities

## Non-goals

- Bundling any of this content in the repository or the APK.
- Automatic background refresh or re-downloads.

## Impact

New `:feature:settings` module and a second, writable Room database. New dependency on WorkManager. Adds foreground
service and notification permissions for the transfer. Contacts `raw.githubusercontent.com`, only when the user starts it.
