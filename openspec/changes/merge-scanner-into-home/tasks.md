# Tasks

## 1. Aiming box and tokens

- [ ] 1.1 `:core:designsystem`: add tokens for the scanner button row and the 3:2 and 45% area rules, and verify the module compiles
- [ ] 1.2 `:feature:pokedex:impl`: move `AimBox` and `AimBoxTest` to the `scanner` package and make `aimBoxFor` near-square, clear of the status bar and buttons; verify with `AimBoxTest` tagged `Crop follows the aiming box` and `Aiming box clear of the controls`

## 2. State and ViewModel

- [ ] 2.1 `:feature:pokedex:impl`: add nested `ScanState` with the continuous `afterFrame` and the new-species rule; verify with reducer tests tagged `Analysis continues during a match`, `Different name replaces the match`, `Same name changes nothing` and `Match stays when the name leaves`
- [ ] 2.2 `:feature:pokedex:impl`: fold the scanner's use cases and intents into `PokedexViewModel` and add the derived `listed`; verify by moving `ScannerViewModelTest` into `PokedexViewModelTest` with `Lock after repeated frames`, `Single frame does not lock`, `Several names offered as choices`, `Nickname keeps scanning`, `Torch toggles`, `Match filters the list`, `Typing replaces a match` and `Clearing a match restores the list`, using fakes from `:core:testing`

## 3. Home screen

- [ ] 3.1 `:feature:pokedex:impl`: move `CameraViewfinder`, `TextFrameAnalyzer`, `MlKitLineRecognizer` and their tests to the `scanner` package, with the CameraX and ML Kit dependencies, `buildConfig`, the camera manifest entries, the device test and the two model files; verify with `./gradlew :feature:pokedex:impl:testDebugUnitTest`
- [ ] 3.2 `:feature:pokedex:impl`: rename the scanner strings to `pokedex_scanner_*`, delete the ones the result sheet used and `pokedex_scan`; verify with `lint`
- [ ] 3.3 `:feature:pokedex:impl`: build the scanner area with camera slot, aiming box, shared hint and match label, button row, and the permission and error messages; verify with screen tests tagged `Hint and aiming box shown`, `Match named in the aiming box`, `Rationale before asking`, `Denied permanently`, `System dialog waits for a tap`, `Search works without the camera`, `Settings opens from the scanner area` and `Torch returns with the camera`
- [ ] 3.4 `:feature:pokedex:impl`: build the sheet layout in `PokedexContent` with `BottomSheetScaffold`, the search field, the list states and the area-height rule; verify with screen tests tagged `Home shows scanner and sheet`, `No separate scan step`, `Short window keeps the list reachable`, `Match opens the entry` and the existing `pokedex-browse` scenario tests
- [ ] 3.5 `:feature:pokedex:impl`: wire focus-to-expand, collapse-ends-search, the back handler, composing the camera only while collapsed, and the status bar rule; verify with screen tests tagged `Tapping search expands the sheet`, `Dragging up expands the sheet`, `Settings stays while searching`, `Expanding stops the camera`, `Collapsing restarts the camera`, `Covering home stops the camera`, `Entry beside the list keeps scanning`, `Wide window keeps list and entry together`, `Collapsing clears the typed text`, `Back collapses the expanded sheet`, `Clearing text keeps the sheet expanded`, `Returning from an entry keeps the search`, `Back clears a match` and `Match kept when the sheet is dragged`
- [ ] 3.6 `:feature:pokedex:impl`: update previews and record phone and tablet Roborazzi goldens for scanning, match, expanded search, permission, loading and dark theme with `./gradlew recordRoborazziDebug`, and review each image

## 4. Remove the separate scanner

- [ ] 4.1 `:feature:scanner:api`, `:feature:scanner:impl`, `:feature:pokedex:impl`, `:app`: delete both scanner modules with their goldens and `ScannerKey`, remove the `onOpenScanner` path, and update `settings.gradle.kts` and the build files; verify with `./gradlew assembleDebug`, and that nothing references `feature:scanner` outside the OpenSpec archive
- [ ] 4.2 Docs: add `docs/DECISIONS/0013-scanner-in-pokedex-feature.md`, update Flow 3 in `docs/MVP_SPEC.md`, the feature list in `CLAUDE.md`, and the README's module table and privacy notes; verify by searching those files for "result sheet" and `:feature:scanner`
- [ ] 4.3 Replace `docs/screenshots/home.png` and `docs/screenshots/scan.png` with captures of the new home screen, and verify the README shows them

## 5. Verify

- [ ] 5.1 Local gate: `./gradlew spotlessApply && ./gradlew spotlessCheck lint coverageVerify assembleDebug -Proborazzi.test.verify=true`
- [ ] 5.2 Run the `android-reviewer` agent on the diff and fix what it finds, then re-run the local gate
- [ ] 5.3 Run the `android-qa` agent for the layout journeys on phone and tablet emulators; camera recognition on real game screens needs the Pixel 6, so ask before installing there

## Workflow follow-up

- Open the PR with the ticked `tasks.md`.
- Archive the change in its own `docs(openspec)` commit after it merges.
