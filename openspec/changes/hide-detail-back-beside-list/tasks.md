# Tasks

## 1. Shared signal (:core:ui)

- [ ] 1.1 :core:ui — add `implementation(libs.androidx.compose.material3.adaptive.navigation3)` and `testImplementation(libs.androidx.navigation3.ui)` to `core/ui/build.gradle.kts` (both already in the catalog); verify `./gradlew :core:ui:compileDebugKotlin` passes
- [ ] 1.2 :core:ui — add `ListDetailPanes.kt` with the public `@Composable fun isDetailShownBesideList(): Boolean` from design.md decision 1: `false` when `LocalListDetailSceneScope.current` is null, otherwise whether `scaffoldTransitionScope.scaffoldStateTransition.targetState[ListDetailPaneScaffoldRole.List]` is not `PaneAdaptedValue.Hidden`; KDoc says it is for detail-pane content only (it also returns true in the list pane and the placeholder) and that it comes from the scene `ListDetailSceneStrategy` drew, never from the window width; verify `./gradlew :core:ui:lintDebug` passes
- [ ] 1.3 :core:ui — Robolectric `ListDetailPanesTest`, the helper's reactivity test: a real `NavDisplay` with `rememberListDetailSceneStrategy(directive = …)` whose directive is a `mutableStateOf` (`maxHorizontalPartitions` 2 or 1) and stand-in entries; the detail stand-in shows its key's id and a `back` marker only when `!isDetailShownBesideList()`:
  - `aDetailAloneShowsItsBackMarker` (one partition: marker shown, list not shown)
  - `aDetailBesideItsListHidesItsBackMarker` (two partitions: list and detail shown, no marker)
  - `// Scenario: Licence Back arrow follows the window size` on `aDetailInItsOwnSceneFollowsTheScene`: a plain root entry, then a `listPane(sceneKey = X)` and a `detailPane(sceneKey = X)` (the Settings → licences shape); two → one → two partitions: the marker appears, then disappears, and the same id is shown throughout
  - `// Scenario: Entry Back arrow follows the window size` on `aDetailInTheDefaultSceneFollowsTheScene`: a root `listPane()` and a `detailPane()` (the home list → entry shape); same flips and checks
  - verify `./gradlew :core:ui:testDebugUnitTest` passes

## 2. Licence pane (:feature:settings:impl)

- [ ] 2.1 :feature:settings:impl — in `SettingsEntries`, inside the `LicenseDetailKey` entry, hoist `val besideList = isDetailShownBesideList()` and pass `onNavigateBack = navigator.backOrNull(key).takeUnless { besideList }` to `LicenseDetailScreen`; the screen, the licences list and its `backFromOrNull` stay unchanged; verify `./gradlew :feature:settings:impl:compileDebugKotlin` passes
- [ ] 2.2 :feature:settings:impl — `LicensesScreenTest`: `showDetail` takes `onBack: (() -> Unit)? = {}`; add `// Scenario: Licence Back arrow on a phone` on `theLicenceBackArrowGoesBackWhenGiven` (tap `licence:back`, the lambda runs) and `// Scenario: No Back arrow on a licence beside the list` on `theLicenceShowsNoBackArrowWithoutOne` (`onBack = null`: no `licence:back`); `tabletDetail` and `tabletNotFound` render with `onBack = null`, as the licence shows beside the list; re-record with `./gradlew :feature:settings:impl:recordRoborazziDebug` and check only the arrow left the two tablet goldens while `phoneDetail`, `phoneNotFound`, `phoneList` and `tabletList` are unchanged; verify `./gradlew :feature:settings:impl:testDebugUnitTest :feature:settings:impl:lintDebug -Proborazzi.test.verify=true` passes

## 3. Entry pane (:feature:entry:impl)

- [ ] 3.1 :feature:entry:impl — `EntryScreen` and `EntryContent` take `showBackArrow: Boolean`; `EntryContent` draws `entry:back` only when `showBackArrow && onNavigateBack != null`, and the `EntryState.Failed` message keeps `onNavigateBack` as its Back action; in `EntryEntries`, hoist `val besideList = isDetailShownBesideList()` and pass `showBackArrow = !besideList`; update `EntryPreviews`; verify `./gradlew :feature:entry:impl:compileDebugKotlin` passes (main sources only: `EntryScreenTest` compiles again after 3.2)
- [ ] 3.2 :feature:entry:impl — `EntryScreenTest`: `show` takes `showBackArrow: Boolean = true`, and the `EntryScreen(...)` call in `rootScreenLoadsFromTheViewModel` passes `showBackArrow = true`; add `// Scenario: Entry Back arrow on a phone` on `theBackArrowGoesBack` (tap `entry:back`, the lambda runs), `// Scenario: No Back arrow on an entry beside the list` on `theBackArrowIsHiddenBesideTheList` (`showBackArrow = false`: no `entry:back`), and `// Scenario: Unknown species` on `aFailedEntryBesideTheListKeepsItsWayBack` (`showBackArrow = false`, `Failed`: "Go back" still runs the lambda); `tablet`, `foldable`, `tabletLoading` and `tabletInGame` render with `showBackArrow = false`; re-record with `./gradlew :feature:entry:impl:recordRoborazziDebug` and check only the arrow left those four goldens while `phone`, `phoneLoading`, `phoneInGame` and `phoneDark` are unchanged; verify `./gradlew :feature:entry:impl:testDebugUnitTest :feature:entry:impl:lintDebug -Proborazzi.test.verify=true` passes

## 4. App integration (:app)

- [ ] 4.1 :app — `AppNavigationTest`: the `LicenseDetailKey` stand-in in `showLicencesFeature()` hoists `val besideList = isDetailShownBesideList()` and shows a `licence:back` "Back" control from `nav.backOrNull(key).takeUnless { besideList }`, as `SettingsEntries` does; add `showEntryFeature()` with a `PokedexKey` `listPane(detailPlaceholder = …)` stand-in and an `EntryKey` `detailPane()` stand-in that shows "Entry #<id>" and an `entry:back` control when `!isDetailShownBesideList()`, as `EntryEntries` does (keep-in-sync KDoc like the licences one); verify it compiles with `./gradlew :app:compileDebugUnitTestKotlin`
- [ ] 4.2 :app — licence tests in `AppNavigationTest`: `// Scenario: No Back arrow on a licence beside the list` on `aLicenceBesideTheListHasNoBackArrowOnAFoldable` (`w841dp-h701dp-xxhdpi`) and `aLicenceBesideTheListHasNoBackArrowOnATablet` (`RobolectricDeviceQualifiers.MediumTablet`): no `licence:back`, `licenses:back` shown; `// Scenario: Licence Back arrow on a phone` on `theLicenceBackArrowReturnsToTheListOnAPhone` (default phone portrait size): `licence:back` shown, tapping it shows "Licences list"; verify `./gradlew :app:testDebugUnitTest --tests '*AppNavigationTest*'` passes
- [ ] 4.3 :app — entry tests in `AppNavigationTest`: `// Scenario: No Back arrow on an entry beside the list` on `anEntryBesideTheListHasNoBackArrowOnAFoldable` and `anEntryBesideTheListHasNoBackArrowOnATablet`: "Home list" and "Entry #1" shown, no `entry:back`; `// Scenario: Entry Back arrow on a phone` on `theEntryBackArrowReturnsToTheHomeListOnAPhone` (default phone portrait size): `entry:back` shown, tapping it shows "Home list"; the existing `foldableShowsBothPanes` golden must stay unchanged; verify `./gradlew :app:testDebugUnitTest --tests '*AppNavigationTest*'` passes
- [ ] 4.4 :app — window size change through recreation in `AppNavigationTest` (the activity declares no `configChanges`, so a fold or resize recreates it):
  - `// Scenario: Licence Back arrow follows the window size` on `theLicenceBackArrowFollowsAFoldAndUnfold`: open a licence at `w841dp-h701dp-xxhdpi` (no `licence:back`); `RuntimeEnvironment.setQualifiers("w411dp-h914dp-xxhdpi")` then `composeRule.activityRule.scenario.recreate()`: `licence:back` shown and "Licence of <same id>" shown; set the foldable qualifiers again and recreate: no `licence:back`, the list and the same licence shown
  - `// Scenario: Entry Back arrow follows the window size` on `theEntryBackArrowFollowsAFoldAndUnfold`: the same steps with an entry: `entry:back` absent, then shown, then absent, and "Entry #<same id>" shown throughout
  - if the compose rule does not bring the content back on the recreated activity, add a test-only activity that calls `setContent { AppTheme { AppNavigation(…) } }` in `onCreate` and drive it with `createAndroidComposeRule<ThatActivity>()` (see design.md risks)
  - verify `./gradlew :app:testDebugUnitTest -Proborazzi.test.verify=true` passes
- [ ] 4.5 :app — on device, ask `android-qa` to run the six new scenarios plus the existing "Licences layout" and "Adaptive layout" ones on a phone (portrait, and landscape where two panes show), a tablet and a foldable emulator (fold and unfold with a licence open and with an entry open), with screenshots; verify the report is PASS

## 5. Gate

- [ ] 5.1 Run `./gradlew spotlessApply && ./gradlew spotlessCheck lint coverageVerify assembleDebug -Proborazzi.test.verify=true` and verify it passes

## Workflow follow-up

- Ask the `android-reviewer` agent to audit the diff before committing.
- The PR description asks the `android-reviewer` (and the human reviewer) to check the real call sites in `SettingsEntries` and `EntryEntries` by eye: the `AppNavigationTest` stand-ins copy their wiring by hand and do not exercise them.
- Open the PR with `Fixes #30` in its body and the ticked tasks.md.
- Archive the change with `/opsx:archive` in its own `docs(openspec)` commit or PR after the implementation is merged.
