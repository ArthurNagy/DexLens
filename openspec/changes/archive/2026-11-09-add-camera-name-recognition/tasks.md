## 1. Domain

- [x] 1.1 `:core:domain`: `SpeciesSignal`, candidates, the name index and matcher, and `LockOnTracker`
- [x] 1.2 `:core:domain`: tests for every matching and lock-on scenario, including a false-positive corpus over real names

## 2. Feature

- [x] 2.1 `/android-forge-add-library` for CameraX and bundled ML Kit Text Recognition
- [x] 2.2 `/android-forge-add-feature scanner`, with the key in `:feature:scanner:api`
- [x] 2.3 `:feature:scanner:impl`: permission states, aiming box, hint and torch
- [x] 2.4 `:feature:scanner:impl`: analyzer with `ViewPort`, crop calculation and ML Kit recognition
- [x] 2.5 `:feature:scanner:impl`: result sheet, haptics, pause and resume, debug overlay
- [x] 2.6 `:feature:scanner:impl`: ViewModel tests, screen tests and phone and tablet goldens

## 3. Privacy and verification

- [x] 3.1 `:app`: remove ML Kit telemetry components from the manifest, with `INTERNET` kept only for the download
- [x] 3.2 Instrumented test on the Pixel 6: real recognition of rendered game-style text through the matcher
- [x] 3.3 Run the local gate: `./gradlew spotlessApply && ./gradlew spotlessCheck lint coverageVerify assembleDebug -Proborazzi.test.verify=true`
