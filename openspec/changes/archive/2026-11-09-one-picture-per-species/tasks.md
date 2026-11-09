## 1. Data

- [x] 1.1 `:core:network`, `:core:data`, `:core:model`: remove sprites and fetch one picture per species
- [x] 1.2 `:core:database`, `:core:domain`, `:core:data`: flavor text as a `Flow`
- [x] 1.3 `:core:data`: expose the artwork location once the pack is complete

## 2. UI

- [x] 2.1 `:core:ui`: load the stored size so every screen shares one decoded picture
- [x] 2.2 `:feature:entry:impl`: collect artwork and flavor text while the entry is open
- [x] 2.3 `:core:designsystem`, `:core:ui`: skeleton components
- [x] 2.4 `:feature:pokedex:impl`, `:feature:entry:impl`: skeletons with phone and tablet goldens

## 3. Tests

- [x] 3.1 Scenario tests for `One picture for each species`, `Art appears when the download finishes`, `Flavor text appears in an open entry`, `Shows loading state` and `Entry shows a skeleton while loading`
- [x] 3.2 Local gate: `./gradlew spotlessApply && ./gradlew spotlessCheck lint coverageVerify assembleDebug -Proborazzi.test.verify=true`
