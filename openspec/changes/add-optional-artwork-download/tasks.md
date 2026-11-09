## 1. Storage and network

- [x] 1.1 `:core:database`: `ContentDatabase` and flavor text DAO with an exported schema
- [x] 1.2 `:core:network`: artwork and flavor text data source, host allowlist interceptor, tests against a mock server
- [x] 1.3 `/android-forge-add-library` for WorkManager

## 2. Domain and data

- [x] 2.1 `:core:domain`: content pack repository, states and use cases
- [x] 2.2 `:core:data`: downloader, image conversion, CSV parsing and import, with tests
- [x] 2.3 `:core:data`: `@HiltWorker`, foreground notification and repository

## 3. Screens

- [x] 3.1 `/android-forge-add-feature settings`: settings screen and first-launch offer
- [x] 3.2 `:feature:pokedex`, `:feature:entry`: show downloaded art and flavor text
- [x] 3.3 `:app`: manifest, `Configuration.Provider`, backup rules; tests and goldens

## 4. Verify

- [x] 4.1 Run the local gate: `./gradlew spotlessApply && ./gradlew spotlessCheck lint coverageVerify assembleDebug -Proborazzi.test.verify=true`
- [x] 4.2 Verify the download, resume and offline use on the Pixel 6
