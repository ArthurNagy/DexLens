# DexLens — MVP Spec

<!--
  Product scope, not behavior: what v1 is meant to be and why. openspec/specs/ records what the app does
  today, one capability at a time. A change outside this file's scope needs the scope decision recorded here first.
-->

## Overview
DexLens is an open-source, offline-first Pokémon reference for Android. All 1,025 species ship inside the app, so
browsing, searching and reading an entry work in airplane mode from the first launch. Pointing the camera at a
Pokémon RPG (Game Boy to Switch) reads the name on screen and opens its entry. It is a public showcase of modern
Android development, freely downloadable from GitHub Releases.

## Target users
Players who want a fast reference next to the game they are playing, without accounts, ads or a connection, and
Android developers reading the code as an example of the current platform stack.

## Business model
None. Free and open source (MIT), distributed as a signed APK on GitHub Releases. Unofficial fan project.

## Must-have (v1)
- All 1,025 species: Pokédex number, names in every language, genus, types, height, weight, base stats, abilities
  including hidden ones, evolution line with triggers, egg groups, gender ratio and forms.
- Type weaknesses, resistances and immunities, computed in the app from the type chart.
- A search-first home: Pokédex-order list, results as you type by prefix, ignoring case and accents, by number and
  in other languages.
- A full entry screen, shown beside the list on tablets and unfolded foldables.
- Camera name recognition: Latin script, on-device only, works offline from first launch.
- An optional, skippable, resumable download of artwork and flavor text. Polished placeholders without it.

## Out of scope (v1)
- Learnsets and move data.
- Recognising Pokémon from images (matching stays behind an interface so a model can be added later).
- Non-Latin text recognition.
- Accounts, sync, ads, analytics and any network use beyond the optional download.
- Wear OS, TV, car and XR surfaces.
- Any official art, sprites or word-for-word game flavor text in the repository or the APK.

## Key user flows

### Flow 1: First launch
1. Open the app. The list is ready, with no setup and no network.
2. A one-time sheet offers the optional download with its size. Skip it or start it.

### Flow 2: Look something up
1. Type a name or number in the search bar. Results update with each letter.
2. Tap a result. The entry opens, with a shared-element transition on phones or beside the list on large screens.

### Flow 3: Scan a game
1. Open the camera and point the aiming box at the Pokémon's name on the game screen.
2. After several frames agree, the phone vibrates and a result sheet appears.
3. Open the entry from the sheet, or pick one when several names are visible.

## Critical technical considerations
- Offline first: no network call apart from the optional download, and the README states the hosts contacted.
- The prebuilt database is generated from Room's exported schema by a reproducible script in `tools/`.
- Text recognition needs about 16×16 pixels per character, so frames are high-resolution and cropped to the aiming box.
- Trademark care: original name and app icon, type-tinted placeholders, no bundled game art.

## Success metrics
- Fresh install works in airplane mode on a Pixel 6: browse, search, entry and camera recognition.
- All android-forge gates pass and the APK contains no game art.
