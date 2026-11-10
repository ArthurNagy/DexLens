Issue: none

## Why

Scanning and searching answer the same question, "which Pokémon is this?", but they live on two screens with two ways of
showing an answer. The scanner is a separate full-screen step behind a button, and its result sheet interrupts the camera
and is the roughest part of the app. Putting the camera on the home screen makes scanning the first thing people see, and
lets both inputs fill one list.

## What Changes

- The home screen shows the camera at the top, about 3:2, with a sheet below it holding the search field and the species list.
- Tapping the search field or dragging the sheet up expands it over the camera and turns the camera fully off. The camera area
  fades into a top bar in the app's surface color, which keeps the settings button.
- Collapsing the sheet, by dragging it down or pressing Back, clears the typed text and turns the camera back on.
- A name the scanner locks onto filters the same list that search uses, and appears in the aiming box. The scanner keeps
  reading, and a different name replaces the match. A clear button in the search field returns to the full list.
- Settings and the torch move to the top end of the camera area. The torch hides while the camera is off.
- The aiming box becomes a near-square centred in the camera area, instead of a thin strip.
- When the camera permission is missing, the camera area explains why and offers a button. The system dialog never appears
  on its own, and search keeps working.
- A switch in Settings, on by default, removes the scanner from the home screen. With it off, the search field and list fill
  the screen under a top bar with the settings button, the camera never starts, and no permission prompt appears.
- **BREAKING** (internal): the Scan button, the separate camera screen, the result sheet and its "Keep scanning" action are
  removed, along with the `ScannerKey` route and the `:feature:scanner` modules. Their code moves into `:feature:pokedex`.

## Capabilities

### New Capabilities
- `home-screen`: the combined home layout, the sheet's two positions, when the camera runs, how a manual search ends, and the
  layout with the scanner switched off.

### Modified Capabilities
- `camera-name-recognition`: matches appear in the home list instead of a result sheet, scanning continues after a match, and
  permission prompts, the torch and the aiming box live in the camera area.
- `app-settings`: Settings gains the switch for the home screen scanner.

## Non-goals

- Image recognition of sprites. The encoder files stay bundled and unused, but the near-square box is the region it will need.
- A history of past scans. Only the current match is listed.
- Changing how names are matched, or how many frames it takes to lock.
- Changing the entry screen, settings, or the list-detail layout on wide windows.

## Impact

- `:feature:pokedex:impl` takes in the camera code, CameraX and ML Kit dependencies, the camera permission and the two model
  files. `:feature:scanner:api` and `:impl` are deleted. `:app`, `:core:designsystem` and `settings.gradle.kts` change to match.
- `:core:domain`, `:core:data`, `:core:datastore`, `:core:testing` and `:feature:settings:impl` gain the scanner preference and
  its switch.
- The model files move to `feature/pokedex/impl/src/main/assets/`, so future model updates are copied there.
- The camera now starts when the app opens, once permission has been granted, unless the switch is off. The README's privacy
  notes and screenshots, and Flow 3 in `docs/MVP_SPEC.md`, are updated.
- A new ADR records why the scanner joined the Pokédex feature.
