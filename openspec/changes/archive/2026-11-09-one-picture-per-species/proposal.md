Issue: none

## Why

Code review of the first version found two gaps. The list showed small sprites and the entry showed official artwork, so
the shared-element transition morphed between two different pictures. And an open screen read downloaded content once,
so art and flavor text only appeared after reopening it. The spinner while a page loads also gave no sense of its shape.

## What Changes

- The download fetches one picture per species, official artwork, and every screen shows it. Sprites are no longer fetched.
- Art appears when the download finishes, and an open entry picks up art and flavor text without being reopened.
- The list and the entry show a shimmering skeleton of their layout while they load.

## Capabilities

### New Capabilities

### Modified Capabilities
- `artwork-download`: one picture per species, and content appears without reopening a screen.
- `pokedex-browse`: the loading state is a skeleton of the list.
- `species-entry`: a skeleton of the entry while it loads.

## Non-goals

- Showing partly downloaded art while the download is running.

## Impact

`:core:data`, `:core:network`, `:core:database`, `:core:domain`, `:core:ui`, `:core:designsystem` and the entry, list and
scanner features. Storage after the download drops from about 25 MB to about 21 MB. The transfer stays about 140 MB.
