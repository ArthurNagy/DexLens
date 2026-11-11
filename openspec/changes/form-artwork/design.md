## Context

Pictures are stored as `content/artwork/<id>.webp` and fetched from PokeAPI's `official-artwork/<id>.png`. Both use the
species id today. In PokeAPI a species' default variety has the same id as the species, and alternate varieties use ids
from 10001 up, which is also how their official artwork is named.

## Decisions

- **Key pictures by variety id.** The downloader lists every variety id instead of every species id. Existing paths,
  the resume logic and the "complete" marker work unchanged, and `ArtworkLocation.artwork(SpeciesId)` still returns the
  default variety's picture. Alternative considered: a separate `forms/` folder keyed by form identifier. It needs a second
  naming scheme and gains nothing.
- **Fall back in the picture slot, not in the data layer.** `ArtworkLocation` only builds paths and never touches the disk,
  so it cannot know that four varieties have no picture. `SpeciesArt` takes an optional fallback path and loads it when
  the first one fails. A missing upstream file is already skipped by the downloader, so the fallback is the only thing
  needed. Alternative considered: recording missing ids in the database. It would need a database build change for four rows.
- **Shared element stays keyed by species.** The list and the entry share the `species-art-<id>` bounds. When a variety is
  selected, `sharedBounds` already crossfades the two pictures, so returning to the list fades from the variety to the
  species picture.

## Risks / Trade-offs

- [Larger download] About 43 MB more. The offer and Settings state the new size before any transfer.
- [Stored size is estimated] The species pictures were measured on a device; the variety pictures use the same PNG to
  WebP ratio. The figure is rounded and can be re-measured after a full download.
