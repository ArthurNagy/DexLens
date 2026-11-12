Issue: none

## Why

Picking Mega Charizard X on Charizard's entry switches the types, stats and abilities, but the picture stays plain
Charizard. The download fetches one picture per species, so the app has nothing else to show. PokeAPI publishes official
artwork for 322 of the 326 alternate varieties in the bundled database (Megas, Gigantamax, regional forms and others).

## What Changes

- The optional download fetches one picture per variety instead of one per species. A species' default variety shares its
  id, so the species pictures keep their names and paths.
- The entry header shows the selected variety's picture, tinted with its types. A variety without official artwork (the
  four Koraidon and Miraidon battle modes) shows the species picture instead.
- The list, the scanner and the evolution tree keep showing the species picture.
- The offer and Settings state the new size: about 183 MB to download and 27 MB on the device, up from 140 MB and 21 MB.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `artwork-download`: one picture per variety instead of one per species.
- `species-entry`: selecting a variety also switches the header picture.

## Non-goals

- Pictures for cosmetic forms that are not their own variety (Unown letters, Vivillon patterns and similar).
- Showing variety pictures in the list, the scanner or the evolution tree.
- Fetching the new pictures for installs that already finished the download. The app has not been published, so there are
  none to migrate.

## Impact

- `:core:model` (`ArtworkLocation`), `:core:database` (`SpeciesDao`), `:core:data` (downloader and repository),
  `:core:domain` (`ContentPackSizes`), `:core:ui` (`SpeciesArt` fallback), `:feature:entry:impl`, `:feature:settings:impl`
  tests and goldens.
- Download grows by about 43 MB, storage by about 6 MB.
