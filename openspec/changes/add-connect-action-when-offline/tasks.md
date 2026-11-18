# Tasks

Every new scenario test carries a `// Scenario: <scenario name>` comment naming the scenario it checks (names as in
this change's delta specs). Tests use the fakes in `:core:testing` and Orbit's test harness, never mocks of our
interfaces.

## 1. Domain and test fakes (:core:domain, :core:testing)

- [x] 1.1 `:core:domain`: add `core.domain.connectivity.ConnectivityRepository` (`fun observeConnectivity(): Flow<Connectivity>`, KDoc: never throws, emits the current value on collection, `Online` = default network has INTERNET and VALIDATED and is not blocked) and `ObserveConnectivityUseCase @Inject constructor(repository) : UseCase` returning it, with `enum class Connectivity { Online, Offline }` in `:core:model` (design D1). Verify `./gradlew :core:domain:compileKotlin` passes.
- [x] 1.2 `:core:testing`: add `FakeConnectivityRepository(connectivity: Connectivity = Connectivity.Online)` exposing `val connectivity = MutableStateFlow(...)` that tests set. Verify `./gradlew :core:testing:compileDebugKotlin` passes.
- [x] 1.3 `:core:domain` test: `connectivity/ObserveConnectivityUseCaseTest` collects the use case over a `FakeConnectivityRepository` and sees `Offline` then `Online` as the fake changes. This is a pass-through test with no scenario marker. Verify `./gradlew :core:domain:test` passes.

## 2. Data (:core:data)

- [x] 2.1 `:core:data`: add internal `core.data.connectivity.AndroidConnectivityRepository` per design D2.
  - It is a `callbackFlow` whose `NetworkCallback` tracks the current default `Network` with its `usable` and `blocked` flags and sends `usable && !blocked`. `usable` means INTERNET && VALIDATED.
  - `onCapabilitiesChanged` makes the network current, `onBlockedStatusChanged` updates `blocked` for the current network, and `onLost` sends false only for the current network.
  - One `catchStorage { }` block gets the `ConnectivityManager` (null fails it), seeds from `activeNetwork` capabilities and calls `registerDefaultNetworkCallback`. On `Err` it sends `Online`.
  - `awaitClose` unregisters only when registration succeeded. The flow ends with `distinctUntilChanged()`.
  - No hand-written try/catch, no `Dispatchers.*`, nothing from `:core:network`.

  Verify it compiles.
- [x] 2.2 `:core:data`: `@Binds` `AndroidConnectivityRepository` to `ConnectivityRepository` in `DataModule`, and declare `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />` in `core/data/src/main/AndroidManifest.xml` with a one-line comment (read-only connectivity check). Verify `./gradlew :app:assembleDebug` passes (the Hilt graph resolves).
- [x] 2.3 `:core:data` Robolectric test `connectivity/AndroidConnectivityRepositoryTest`. It uses `shadowOf(connectivityManager)`: `setDefaultNetworkActive(true)` with `setActiveNetworkInfo(...)` for an active network, `setNetworkCapabilities(network, ShadowNetworkCapabilities.newInstance().apply { addCapability(...) })` for its capabilities, and `getNetworkCallbacks()` to reach the registered callback and drive it. It checks that:
  - an active network with INTERNET+VALIDATED emits `Online` first;
  - INTERNET without VALIDATED emits `Offline` (`// Scenario: Network that needs sign-in counts as offline`);
  - no active network emits `Offline`;
  - `onCapabilitiesChanged` / `onLost` on the callback emit `Online` / `Offline` without repeats;
  - `onLost` for a network other than the current one emits nothing;
  - `onBlockedStatusChanged(network, true)` emits `Offline`;
  - a `ContextWrapper` whose `getSystemService` returns a null `ConnectivityManager` makes the flow emit `Online` without throwing;
  - `onCapabilitiesChanged` for a different network after a blocked one resets `blocked` and emits `Online`;
  - cancelling collection leaves `getNetworkCallbacks()` empty, and cancelling after a failed registration doesn't throw. A failed registration is reachable only through the null-`ConnectivityManager` `ContextWrapper`, because Robolectric's `registerDefaultNetworkCallback` never throws.

  Verify `./gradlew :core:data:testDebugUnitTest` passes.

## 3. Presentation: state and ViewModels (:feature:settings:impl)

- [x] 3.1 `ContentOfferState` gains `connectivity: Connectivity` (`Initial` = `Online`) and `networkSettingsUnavailable: Boolean` (`Initial` = `false`). `ContentOfferViewModel` takes `ObserveConnectivityUseCase` and collects it from the on-create block inside `repeatOnSubscription { }`. The same `reduce` clears `networkSettingsUnavailable` when `connectivity` becomes `Online`. The ViewModel adds `onNetworkSettingsUnavailable()` (sets the flag) and `onNetworkSettingsOpened()` (clears it) (design D3, D4). Add the three strings from design D6 to `feature/settings/impl/src/main/res/values/strings.xml`. Verify it compiles.
- [x] 3.2 `SettingsState` gains `connectivity: Connectivity` (`Initial` = `Online`). `SettingsViewModel` takes `ObserveConnectivityUseCase`, collects it the same way, and adds `onNetworkSettingsUnavailable()` setting `pendingMessage = R.string.settings_network_settings_unavailable`. Verify it compiles.
- [x] 3.3 In the same change as 3.1 and 3.2, update the call sites that the new constructors and state break:
  - `SettingsViewModelTest.settings()` and `SettingsViewModelTest.offer()`;
  - `SettingsScreenTest.settingsViewModel()`;
  - `SettingsViewModelTest.offerFollowsProgress`, which builds `ContentOfferState(ContentPackState.Downloading(1, 2))` directly and becomes `ContentOfferState.Initial.copy(content = ContentPackState.Downloading(1, 2))`.

  Each factory passes `ObserveConnectivityUseCase(FakeConnectivityRepository())`. Verify `./gradlew :feature:settings:impl:testDebugUnitTest` passes with no other test changed.
- [x] 3.4 `SettingsViewModelTest` (Orbit `testWithInternalState`), checking that:
  - the offer goes from offline to connected and back with `connectivity` Offline → Online → Offline, tagged `// Scenario: Offer while offline`, `// Scenario: Offer shows Download once connected` and `// Scenario: Offer goes back offline`;
  - an offline offer still marks itself answered and starts nothing (`// Scenario: Skip while offline`);
  - the offer's `onNetworkSettingsUnavailable()` sets `networkSettingsUnavailable`, `onNetworkSettingsOpened()` clears it, and the connection returning also clears it (`// Scenario: Offer can't open network settings`);
  - Settings follows offline → connected → offline (`// Scenario: Download from settings while offline` and `// Scenario: Settings download returns when connected`);
  - Settings' `onNetworkSettingsUnavailable()` sets the message once and clears it (`// Scenario: Connect from settings`).

  Verify `./gradlew :feature:settings:impl:testDebugUnitTest` passes.

## 4. Presentation: screens, the connect action and goldens (:feature:settings:impl)

- [x] 4.1 Add `rememberOpenNetworkSettings(onUnavailable): () -> Unit` next to `rememberOpenLink` in `SettingsScreen.kt` (design D5). It tries `Settings.Panel.ACTION_INTERNET_CONNECTIVITY`, then `Settings.ACTION_WIRELESS_SETTINGS`, then `onUnavailable`, catching each `ActivityNotFoundException`, and holds the callback with `rememberUpdatedState`. Verify it compiles.
- [x] 4.2 `ContentOfferContent` takes `connectivity`, `networkSettingsUnavailable` and `onConnect` (design D6).
  - `NotDownloaded` + offline shows `SizeText()`, `OfflineText()` (polite live region), "Connect to a network" (`offer:connect`), the inline message `Text` (`offer:message`, polite live region) while `networkSettingsUnavailable`, and "Not now" (`offer:skip`), with no `offer:download`.
  - Other branches stay unchanged, with no snackbar host.
  - `ContentOfferDialog` wires `onConnect = rememberOpenNetworkSettings(onUnavailable = viewModel::onNetworkSettingsUnavailable, onOpen = viewModel::onNetworkSettingsOpened)` (one outcome callback per tap).

  In the same task, update the 5 positional `ContentOfferContent(...)` calls in `SettingsScreenTest` (`offerShowsSizeAndChoices`, `offerShowsProgressAndContinuesInBackground`, `offerResumesAndCompletes`, `offerClosesWhenReady`, and the `offer` golden), passing `connectivity = Connectivity.Online, networkSettingsUnavailable = false, onConnect = {}`. Add an offline-offer preview to `SettingsPreviews.kt`. Verify `./gradlew :feature:settings:impl:testDebugUnitTest -Proborazzi.test.verify=true` passes, which shows the existing `offer` golden unchanged.
- [x] 4.3 `ContentPackSection` and `SettingsContent` take `connectivity` / `onConnect` (design D6).
  - Offline `NotDownloaded` shows `SizeText()`, `OfflineText()` and "Connect to a network" (`settings:connect`).
  - Offline `Interrupted` shows the progress, the interrupted line, `OfflineText()`, then Connect (`settings:connect`) and Delete (`settings:delete`) in a `FlowRow` with `spacedBy(AppSpacing.sm)` both ways.
  - `Downloading`, `Ready` and the online rows stay unchanged.
  - `SettingsScreen` wires `rememberOpenNetworkSettings(viewModel::onNetworkSettingsUnavailable)`.

  In the same task, update the positional `SettingsContent(...)` calls in `SettingsScreenTest.show()`, `SettingsScreenTest.theSourceRowHandsTheLinkToAnotherApp` and `SettingsPreviews.kt`. Add an offline-Settings preview. Verify `./gradlew :feature:settings:impl:testDebugUnitTest -Proborazzi.test.verify=true` passes, which shows the existing Settings goldens unchanged.
- [x] 4.4 `SettingsScreenTest` for the offer and the shared helper, checking that:
  - the offline offer shows the size and "No internet connection.", and that `offer:connect` and `offer:skip` exist while `offer:download` and `offer:message` do not (`// Scenario: Offer while offline`);
  - `offer:skip` on the offline offer calls `onSkip` (`// Scenario: Skip while offline`);
  - tapping `offer:connect` wired to `rememberOpenNetworkSettings` starts an intent with action `Settings.Panel.ACTION_INTERNET_CONNECTIVITY` (`// Scenario: Connect opens the internet panel`);
  - with `checkActivities(true)` and a `ResolveInfo` added for `ACTION_WIRELESS_SETTINGS` only, `rememberOpenNetworkSettings` starts `ACTION_WIRELESS_SETTINGS` (`// Scenario: Network settings as the fallback` and `// Scenario: Connect from settings`; Settings uses this same helper, so this test also covers its fallback step);
  - with `checkActivities(true)` and nothing resolvable, `ContentOfferDialog` with a real `ContentOfferViewModel` shows "Network settings can't be opened on this device." inline under Connect and keeps the offer open. Then, with the message showing, the test makes the panel intent resolvable (a `ResolveInfo` for `ACTION_INTERNET_CONNECTIVITY`), taps `offer:connect`, and asserts that `offer:message` is gone and the panel intent started (`// Scenario: Offer can't open network settings`);
  - with a real `ContentOfferViewModel` over `FakeConnectivityRepository`, flipping it to `Online` shows `offer:download` and flipping it back shows `offer:connect`, without re-setting content (`// Scenario: Offer shows Download once connected` and `// Scenario: Offer goes back offline`).

  Verify `./gradlew :feature:settings:impl:testDebugUnitTest` passes.
- [x] 4.5 `SettingsScreenTest` for Settings, checking that:
  - offline `NotDownloaded` shows the size, "No internet connection." and `settings:connect`, with no `settings:download` (`// Scenario: Download from settings while offline`);
  - offline `Interrupted` shows the progress, `settings:connect` and `settings:delete`, with no `settings:resume` (`// Scenario: Resume from settings while offline`);
  - with a real `SettingsViewModel` over `FakeConnectivityRepository`, Download comes back on connect and goes away on disconnect (`// Scenario: Settings download returns when connected`);
  - `settings:connect` starts the panel intent, and with nothing resolvable, `SettingsScreen` shows the network-settings message in its snackbar (`// Scenario: Connect from settings`). The middle step, network settings, is covered by the shared-helper fallback test in 4.4.

  Verify `./gradlew :feature:settings:impl:testDebugUnitTest` passes.
- [x] 4.6 Roborazzi goldens in `SettingsScreenTest`:
  - `offerOffline` (Pixel7) and `offerOfflineTablet` (MediumTablet) for the offline offer;
  - `offerOfflineMessage` (Pixel7) for the offline offer with the inline message showing;
  - `phoneOffline` and `tabletOffline` for offline `NotDownloaded` Settings;
  - `phoneInterruptedOffline` and `tabletInterruptedOffline` for offline `Interrupted` Settings. On the phone, Delete wraps under Connect.

  Run `./gradlew recordRoborazziDebug` and check that the new PNGs show the offline branches as designed. `git status` must show only new PNGs: the existing `offer`, `phone`, `tablet`, `foldable` and other goldens are byte-identical. Commit the new PNGs.

## 5. Local gate (all modules)

- [x] 5.1 Run `./gradlew unitTests` and verify it is green.
- [x] 5.2 Run `./gradlew spotlessApply && ./gradlew spotlessCheck lint coverageVerify assembleDebug -Proborazzi.test.verify=true` and verify it passes. If coverage falls short, add the missing `AndroidConnectivityRepository` or composable branch tests, never an exclusion.
- [x] 5.3 Permission check (`// Scenario: Connection check stays on the device`).
  - On this branch and on `origin/main`, run `./gradlew :app:processReleaseMainManifest`, then `grep -o 'uses-permission android:name="[^"]*"' app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml | sort -u` (adjust the path if AGP places it elsewhere).
  - Verify both sorted, unique lists are identical and include `android.permission.ACCESS_NETWORK_STATE`.
  - Paste both lists in the PR body.
- [x] 5.4 Ask the `android-reviewer` agent to audit `git diff origin/main...HEAD`, including that `core.data.connectivity` imports nothing from `:core:network` and makes no network call (the review half of "Connection check stays on the device"). Fix every blocking finding before opening the PR.

## 6. Acceptance on a device (android-qa)

- [ ] 6.1 Ask the `android-qa` agent to QA `add-connect-action-when-offline` on emulators, driving them only through the Android CLI and capturing a screenshot at each step. Use a current-API phone emulator plus a tablet for the layout, and also an API 29 or 30 emulator for the "Tap Connect" journey, where the panel is Settings' `SettingsPanelActivity`. The journeys:
  - fresh install with airplane mode on: the offer shows the size, "No internet connection.", "Connect to a network" and "Not now";
  - tap Connect: the internet panel opens over DexLens on the current API and on API 29/30. If it doesn't open on API 29/30, apply design D5's activity-result launcher fallback and re-run;
  - turn airplane mode off in the panel and return: the offer switches to Download within a few seconds, without being reopened;
  - airplane mode on again before tapping: the offer goes back to Connect;
  - "Not now" while offline: no offer after a relaunch, and the list, search, an entry and the camera work with placeholders;
  - Settings in airplane mode: Connect replaces Download; with an interrupted download, Connect replaces Resume and Delete stays (wrapping on the phone); turning airplane mode off brings Download or Resume back without leaving Settings.

  Verify android-qa returns PASS.

  Status (2026-11-18): not ticked. On the phone (medium_phone, current API) all 6 journeys passed. On API 30 (dexlens_qa_api30) the offer, the panel and the switch back to Download passed. A plain `startActivity` opened `SettingsPanelActivity` there, so the activity-result launcher from design D5 isn't needed. The tablet journeys never ran: the medium_tablet emulator dropped off adb while the APK was installing and crashed twice on restart. The tablet layouts are covered by the `offerOfflineTablet`, `tabletOffline` and `tabletInterruptedOffline` goldens. Re-run the tablet journeys on a working tablet emulator before ticking.
- [ ] 6.2 Ask the maintainer to check criterion 2 (Connect → the Internet panel) on one real Android 12+ device as well, since OEMs differ (issue risk). DexLens never installs to a physical device without asking first. Record the device and the result in the PR body.

  Status: not ticked. This check belongs to the maintainer, on a real device.

## Workflow follow-up

- Open the PR with `Fixes #33` in the body and the ticked tasks.md.
- After it merges, archive the change with `/opsx:archive` in its own `docs(openspec)` commit.
