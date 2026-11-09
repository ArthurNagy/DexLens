## Context

Sprites were 96-pixel PNGs, only used by the list, evolution rows and scan results. Each of those already has room for
the 384-pixel artwork, which is what the entry shows.

## Goals / Non-Goals

**Goals:** the same picture everywhere for a species, and downloaded content appearing as soon as it is complete.

**Non-Goals:** showing art from an unfinished download.

## Decisions

- **Form factors:** `:app` on phone, tablet and foldable. No layout changes beyond the skeletons, which follow each screen's own shape.
- **One picture.** Artwork only. Coil decodes it at its stored size everywhere, so the list and the entry share one memory-cache entry and the hero has no decode gap.
- **The location appears on completion.** `ArtworkLocation` is null until the pack is ready. A file that was missing when a row was first drawn would otherwise never be retried, because its path does not change.
- **Flavor text is a stream.** `ContentDao` returns a Room `Flow` and the entry collects it with the artwork location. Species data stays one-shot: the prebuilt database is read-only.
- **Skeleton.** `SkeletonShimmer` in `:core:designsystem` draws one sweep across its blocks and only reads the animation while drawing, so it never recomposes the page.

## Risks / Trade-offs

- A download stopped partway shows placeholders until it is resumed and finished. Finished files are still kept and not fetched again.
