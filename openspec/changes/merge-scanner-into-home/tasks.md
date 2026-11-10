# Tasks

## 1. Aiming box and tokens

- [x] 1.1 `:core:designsystem`: add tokens for the scanner button row and the 3:2 and 45% area rules, and verify the module compiles
- [x] 1.2 `:feature:pokedex:impl`: move `AimBox` and `AimBoxTest` to the `scanner` package and make `aimBoxFor` use most of the area, clear of the status bar and buttons; verify with `AimBoxTest` tagged `Crop follows the aiming box` and `Aiming box clear of the controls`

## 2. State and ViewModel

- [x] 2.1 `:feature:pokedex:impl`: add nested `ScanState` with the continuous `afterFrame` and the new-species rule; verify with reducer tests tagged `Analysis continues during a match`, `Different name replaces the match`, `Same name changes nothing` and `Match stays when the name leaves`
- [x] 2.2 `:feature:pokedex:impl`: fold the scanner's use cases and intents into `PokedexViewModel` and add the derived `listed`; verify by moving `ScannerViewModelTest` into `PokedexViewModelTest` with `Lock after repeated frames`, `Single frame does not lock`, `Several names offered as choices`, `Nickname keeps scanning`, `Torch toggles`, `Match filters the list`, `Typing replaces a match` and `Clearing a match restores the list`, using fakes from `:core:testing`

## 3. Home screen

- [x] 3.1 `:feature:pokedex:impl`: move `CameraViewfinder`, `TextFrameAnalyzer`, `MlKitLineRecognizer` and their tests to the `scanner` package, with the CameraX and ML Kit dependencies, `buildConfig`, the camera manifest entries, the device test and the two model files; verify with `./gradlew :feature:pokedex:impl:testDebugUnitTest`
- [x] 3.2 `:feature:pokedex:impl`: rename the scanner strings to `pokedex_scanner_*`, delete the ones the result sheet used and `pokedex_scan`; verify with `lint`
- [x] 3.3 `:feature:pokedex:impl`: build the scanner area with camera slot, aiming box, shared hint and match label, button row, and the permission and error messages; verify with screen tests tagged `Hint and aiming box shown`, `Match named in the aiming box`, `Rationale before asking`, `Denied permanently`, `System dialog waits for a tap`, `Search works without the camera`, `Settings opens from the scanner area` and `Torch returns with the camera`
- [x] 3.4 `:feature:pokedex:impl`: build the sheet layout in `PokedexContent` with `BottomSheetScaffold`, the search field, the list states and the area-height rule; verify with screen tests tagged `Home shows scanner and sheet`, `No separate scan step`, `Short window keeps the list reachable`, `Match opens the entry` and the existing `pokedex-browse` scenario tests
- [x] 3.5 `:feature:pokedex:impl`: wire focus-to-expand, collapse-ends-search, the back handler, composing the camera only while collapsed, and the status bar rule; verify with screen tests tagged `Tapping search expands the sheet`, `Dragging up expands the sheet`, `Settings stays while searching`, `Expanding stops the camera`, `Collapsing restarts the camera`, `Covering home stops the camera`, `Entry beside the list keeps scanning`, `Wide window keeps list and entry together`, `Collapsing clears the typed text`, `Back collapses the expanded sheet`, `Clearing text keeps the sheet expanded`, `Returning from an entry keeps the search`, `Back clears a match` and `Match kept when the sheet is dragged`
- [x] 3.6 `:feature:pokedex:impl`: update previews and record phone and tablet Roborazzi goldens for scanning, match, expanded search, permission, loading and dark theme with `./gradlew recordRoborazziDebug`, and review each image
- [x] 3.7 `:feature:pokedex:impl`, `:core:designsystem`: round the aiming box's corners and fade the camera area into a surface-colored top bar as the sheet expands, with status bar icons following it; verify with screen tests tagged `Top bar above the expanded sheet` and `Camera fades into the top bar`, and re-record the goldens

## 4. Scanner switch

- [x] 4.1 `:core:domain`, `:core:datastore`, `:core:data`, `:core:testing`: add the `scannerEnabled` preference, true until changed, with its observe and set use cases and the fake; verify with `UserPreferencesDataSourceTest`, `OfflineUserPreferencesRepositoryTest` and the use-case tests
- [x] 4.2 `:feature:settings:impl`: add the switch row, its state, intent and string; verify with `SettingsViewModelTest` and a `SettingsScreenTest` tagged `Scanner switch applies`, and re-record the Settings goldens
- [x] 4.3 `:feature:pokedex:impl`: observe the preference as a nullable `scannerEnabled`, and clear the match, lock history and torch when it turns off; verify with ViewModel tests tagged `Scanner on by default` and `Turning the scanner off clears a match`
- [x] 4.4 `:feature:pokedex:impl`: show only the background until the value is known, add the plain layout for the switched-off state, and start the sheet expanded when there is typed text; verify with screen tests tagged `Scanner off shows only the list`, `Search works with the scanner off`, `Back clears typed text with the scanner off`, `Back leaves with the scanner off`, `Camera stays off at launch`, `No permission prompt with the scanner off`, `Turning the scanner off keeps a search`, `Turning the scanner on shows the camera` and `Turning the scanner on during a search`
- [x] 4.5 `:feature:pokedex:impl`: record phone and tablet Roborazzi goldens for the switched-off state with `./gradlew recordRoborazziDebug`, and review each image

## 5. Remove the separate scanner

- [x] 5.1 `:feature:scanner:api`, `:feature:scanner:impl`, `:feature:pokedex:impl`, `:app`: delete both scanner modules with their goldens and `ScannerKey`, remove the `onOpenScanner` path, and update `settings.gradle.kts` and the build files; verify with `./gradlew assembleDebug`, and that nothing references `feature:scanner` outside the OpenSpec archive
- [x] 5.2 Docs: add `docs/DECISIONS/0013-scanner-in-pokedex-feature.md`, update Flow 3 in `docs/MVP_SPEC.md`, the feature list in `CLAUDE.md`, and the README's module table and privacy notes, saying how to turn the scanner off; verify by searching those files for "result sheet" and `:feature:scanner`
- [x] 5.3 Replace `docs/screenshots/home.png` and `docs/screenshots/scan.png` with captures of the new home screen, and verify the README shows them

## 6. Verify

- [x] 6.1 Local gate: `./gradlew spotlessApply && ./gradlew spotlessCheck lint coverageVerify assembleDebug -Proborazzi.test.verify=true`
- [x] 6.2 Run the `android-reviewer` agent on the diff and fix what it finds, then re-run the local gate
- [ ] 6.3 Run the `android-qa` agent for the layout journeys on phone and tablet emulators; camera recognition on real game screens needs the Pixel 6, so ask before installing there

## Workflow follow-up

- Open the PR with the ticked `tasks.md`.
- Archive the change in its own `docs(openspec)` commit after it merges.
