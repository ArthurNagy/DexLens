# Tasks

## 1. Data

- [x] 1.1 `:core:database`: add `pictureIds()` and `pictureCount()` over the variety table to `SpeciesDao`
- [x] 1.2 `:core:data`: download and count pictures by variety id; verify with `ContentPackDownloaderTest` tests tagged `One picture for each variety` and `Variety without official artwork is skipped`
- [x] 1.3 `:core:model`: `ArtworkLocation.artwork(Variety)`, verified with an `ArtworkLocationTest` tagged `Species screens use the default variety`; `:core:domain`: update `ContentPackSizes`

## 2. Screens

- [x] 2.1 `:core:ui`: `SpeciesArt` takes a fallback path used when the first picture fails to load
- [x] 2.2 `:feature:entry:impl`: the header shows the selected variety's picture with the species picture as fallback; verify with screen tests tagged `Selecting a variety shows its picture` and `Variety without its own picture`
- [x] 2.3 `:feature:settings:impl`: update the size text in tests; re-record goldens with `./gradlew recordRoborazziDebug` and review each changed image

## 3. Check

- [x] 3.1 Run the local gate: `./gradlew spotlessApply && ./gradlew spotlessCheck lint coverageVerify assembleDebug -Proborazzi.test.verify=true`
- [x] 3.2 On the emulator, download the artwork and check Charizard's three varieties and a Koraidon battle mode
