# Tasks

## 1. Data

- [x] 1.1 `:core:database`: add the new entities, extend `GameVersionEntity`, add `GameDao`, bump `DexDatabase` to version 2 and export the schema
- [x] 1.2 `tools/build_database.py`: build the version groups, version names, moves, past move values, learnsets, encounters, location names and methods from the pinned commit, with `learnset` and `encounter` as `WITHOUT ROWID`; add validation checks; regenerate `dexlens.db`
- [x] 1.3 `:core:database`: extend `PrebuiltDatabaseTest`; verify with tests tagged `Learnsets bundled`, `Move as of the game`, `Locations bundled` and `Prebuilt file passes validation`

## 2. Domain

- [x] 2.1 `:core:model`: add `Game`, `LevelUpMove`, `WildEncounter`, `GameDetails`, `InGame`; remove `SpeciesDetail.otherNames`
- [x] 2.2 `:core:domain`: add `GameRepository`, `GetInGameDetailsUseCase`, `SetPreferredGameUseCase` and the `preferredGame` preference; verify with use-case tests tagged `Last picked game is remembered` and `Newest game by default`, using fakes from `:core:testing`
- [x] 2.3 `:core:testing`: add `FakeGameRepository` and the preference to `FakeUserPreferencesRepository`

## 3. Data layer

- [x] 3.1 `:core:datastore`, `:core:data`: store `preferredGame`; verify with `UserPreferencesDataSourceTest` and `OfflineUserPreferencesRepositoryTest`
- [x] 3.2 `:core:data`: add `OfflineGameRepository` with the default-variety fallback and encounter grouping; verify with tests tagged `Only games with data are offered`, `Moves learnt by level up`, `Where to find`, `Version-exclusive location`, `Variety without its own moves` and `Variety with its own moves`
- [x] 3.3 `:core:data`: order flavor entries newest first and drop `otherNames` from `OfflineSpeciesRepository`; update their tests

## 4. Entry

- [x] 4.1 `:feature:entry:impl`: replace `flavor` with `description`, add the in-game state and intents; verify with ViewModel tests tagged `Description from downloaded text`, `Picking a game updates the section`, `Last picked game is remembered` and `Flavor text appears in an open entry`
- [x] 4.2 `:feature:entry:impl`: remove the Pokédex entries and other names sections, add the description and the "In game" section with picker, moves and locations; verify with screen tests tagged `No names in other languages`, `No description without the download`, `Entry without flavor text`, `Moves learnt by level up`, `Where to find`, `No location data for the game` and `Not found in the wild`
- [x] 4.3 `:feature:entry:impl`: update previews and record phone and tablet goldens with `./gradlew recordRoborazziDebug`, and review each image

## 5. App and docs

- [x] 5.1 `:app`: give `predictivePopTransitionSpec` the pop fade; verify with an `AppNavigationTest` tagged `Back gesture matches the back arrow`, and on the emulator with a recording
- [x] 5.2 `docs/MVP_SPEC.md`, `README.md`: move level-up learnsets and wild locations into scope and describe the new section
- [x] 5.3 Run the local gate: `./gradlew spotlessApply && ./gradlew spotlessCheck lint coverageVerify assembleDebug -Proborazzi.test.verify=true`
