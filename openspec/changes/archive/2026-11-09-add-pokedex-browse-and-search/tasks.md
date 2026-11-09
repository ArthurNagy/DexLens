## 1. Domain

- [x] 1.1 `:core:model`: species, types, stats, abilities, varieties, forms, evolution and matchup types
- [x] 1.2 `:core:domain`: repositories, name normalizer, matchup calculator and use cases
- [x] 1.3 `:core:testing`: fakes and builders; `:core:domain` tests tagged with scenarios

## 2. Data

- [x] 2.1 `:core:data`: `OfflineSpeciesRepository`, `OfflineTypeChartRepository`, mappers, `DataModule` bindings and tests

## 3. Design system

- [x] 3.1 `:core:designsystem`: brand color schemes, type colors, type chip, stat bar and species art placeholder

## 4. Features

- [x] 4.1 `:feature:pokedex`: home list and search through `/android-forge-add-feature`, ViewModel, screen, tests, goldens
- [x] 4.2 `:feature:entry`: full entry through `/android-forge-add-feature`, ViewModel, screen, tests, goldens
- [x] 4.3 `:app`: start destination, shared-element layout and list-detail wiring

## 5. Verify

- [x] 5.1 Run the local gate: `./gradlew spotlessApply && ./gradlew spotlessCheck lint coverageVerify assembleDebug -Proborazzi.test.verify=true`
- [x] 5.2 Verify browse, search and entry on the Pixel 6
