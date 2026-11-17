# Tasks

## 1. Model and domain (:core:model, :core:domain, :core:testing)

- [x] 1.1 :core:model — add `OpenSourceLibrary`, `OpenSourceLibraryDetail` and `LibraryLicense` (no defaults); verify `./gradlew :core:model:compileKotlin` passes
- [x] 1.2 :core:domain — add `OpenSourceLibraryRepository` and `AppInfoRepository` in a new `about` package, and `GetOpenSourceLibrariesUseCase`, `GetOpenSourceLibraryUseCase`, `GetAppVersionUseCase`, one per file; verify `./gradlew :core:domain:compileKotlin` passes
- [x] 1.3 :core:testing — add `FakeOpenSourceLibraryRepository` (settable libraries, details by id, `NotFound` for unknown ids, settable failure) and `FakeAppInfoRepository`, plus a small fixture list; verify they compile with `./gradlew :core:testing:compileDebugKotlin`
- [x] 1.4 :core:domain — use-case tests over the fakes (list passes through, unknown id gives `NotFound`, version passes through); verify `./gradlew :core:domain:test` passes

## 2. Data (:core:data)

- [ ] 2.1 :core:data — add `aboutlibraries-core` (already in the catalog) to `implementation`; add the internal `NoticesSource` and `AndroidNoticesSource` (raw resource found by name, `DiscouragedApi` suppressed with a comment pointing at `:app`'s `keep.xml`; assets through `AssetManager`); verify `./gradlew :core:data:compileDebugKotlin` and `:core:data:lintDebug` pass
- [ ] 2.2 :core:data — implement `@Singleton` `BundledOpenSourceLibraryRepository`: parse once with `Libs.Builder().withJson` on `DispatcherProvider.io` inside `catchStorage`, cache under a `Mutex`, sort by name ignoring case, map holder (organisation, else developers, else null), strip template copyright lines (trimmed line starts with `Copyright`, case-insensitive, and contains both `<year>` and `<owner>`), attach embedded notices by `group:artifact` from `embedded_notices/index.json` skipping `third_party_licenses.json`, `NotFound` for an unknown id
- [ ] 2.3 :core:data — implement `PackageAppInfoRepository` (`getPackageInfo(packageName, 0).versionName` inside `catchStorage`, null → `DomainError.Unknown`) and bind both repositories in `DataModule`; verify `./gradlew :app:assembleDebug` resolves the Hilt graph
- [ ] 2.4 :core:data — repository tests with a fake `NoticesSource` over a JSON fixture in #22's format: `// Scenario: Libraries listed with their licences` (names, versions, licence names, ordering), `// Scenario: Licence text shown`, `// Scenario: Copyright holder shown` (holder mapped; both the BSD `Copyright (c) <year> <owner>. ` and the ISC `Copyright <year> <owner> ` lines removed; a real copyright line kept), `// Scenario: Bundled notices shown` (notice text attached once, the `.json` index ignored), `// Scenario: Unknown library` (`NotFound`), `// Scenario: Licences listed offline` and `// Scenario: Licence text offline` (the repository reads only the source, a source failure becomes `DomainError.Unknown`); a Robolectric test for `PackageAppInfoRepository` that installs a `PackageInfo` with a known version through `ShadowPackageManager` and checks it is returned, and that a null version gives `DomainError.Unknown`; a Robolectric test that `AndroidNoticesSource` with no `aboutlibraries` resource makes the repository return `DomainError.Unknown`; verify `./gradlew :core:data:testDebugUnitTest` passes

## 3. Navigation (:core:navigation, :feature:settings:api)

- [ ] 3.0 :core:navigation — add `Navigator.backFrom(key)` (removes `key` and every entry above it, never the root) and `backFromOrNull(key)` (null for the root, like `backOrNull`); `NavigatorTest` cases for a detail above the key, the key on top, and the root; verify `./gradlew :core:navigation:test` passes
- [ ] 3.1 :feature:settings:api — add `LicensesKey` and `LicenseDetailKey(libraryId: String)`, both `@Serializable`; verify `./gradlew :feature:settings:api:compileDebugKotlin` passes

## 4. Settings About section (:feature:settings:impl)

- [ ] 4.1 :feature:settings:impl — strings with the `settings_` prefix: version ("Version %1$s"), full credits (PokeAPI BSD-3-Clause, ML Kit text recognition, LiteRT, "Image recognition uses a MobileNetV4 model (timm, Apache-2.0), fine-tuned for DexLens.", open source under the MIT License), "Open-source licences", "Source code", the source URL (`translatable="false"`), the link-unavailable message, the licences title, the detail placeholder, "Copyright: %1$s" and the library-not-found message; UK "licence" for the noun
- [ ] 4.2 :feature:settings:impl — `SettingsState` gains `versionName` and `@StringRes pendingMessage`; `SettingsViewModel` loads the version once at start and adds `onSourceLinkUnavailable()` and `onMessageConsumed()`; ViewModel tests with `// Scenario: Version shown`, `// Scenario: Version unavailable` (failure leaves `versionName` null) and `// Scenario: No app for the source link` (message set, then cleared); verify `./gradlew :feature:settings:impl:testDebugUnitTest --tests '*SettingsViewModelTest*'` passes
- [ ] 4.3 :feature:settings:impl — About section: version (only when known), credits, legal, then clickable "Open-source licences" (`settings:licenses`) and "Source code" (`settings:source`) rows; the screen starts `ACTION_VIEW`, catches `ActivityNotFoundException` and calls `onSourceLinkUnavailable()`; the snackbar shows `pendingMessage`; `SettingsScreen` gets an `onOpenLicences` lambda wired to `navigator.navigate(LicensesKey)` in `SettingsEntries`
- [ ] 4.4 :feature:settings:impl — screen tests: `// Scenario: Credits shown` (all credits and the fan-project notice), `// Scenario: Version shown`, `// Scenario: Version unavailable` (no version line), `// Scenario: Licences opened from settings` (the lambda is called), `// Scenario: Source code opened in the browser` (the started intent is `ACTION_VIEW` on the URL), `// Scenario: No app for the source link` (Robolectric `shadowOf(application).checkActivities(true)`, message shown, no crash); re-record the phone and tablet Settings goldens; verify `./gradlew :feature:settings:impl:testDebugUnitTest` passes

## 5. Licences screens (:feature:settings:impl)

- [ ] 5.1 :feature:settings:impl — `LicensesViewModel` (loads the list, loading and error state) and `LicensesScreen` (`LazyColumn` of `ListItem`s keyed by id, name then version and licence names, Back from `navigator.backFromOrNull(key)`); ViewModel test with `// Scenario: Libraries listed with their licences`; verify the test passes
- [ ] 5.2 :feature:settings:impl — assisted `LicenseDetailViewModel` (`@HiltViewModel(assistedFactory = …)`, loads the detail, builds `rows: List<LicenceRow>` — `Holder`, `SectionTitle`, `Paragraph` — off the main thread on `DispatcherProvider.default`, splitting texts on blank lines and keeping only the rows; `notFound` for `NotFound`, `pendingError` otherwise) and `LicenseDetailScreen` (`LazyColumn` over the rows: name and version header, "Copyright: holder", each licence title and paragraphs, then each notice title and paragraphs; not-found message instead of the rows); ViewModel tests with `// Scenario: Licence text shown`, `// Scenario: Bundled notices shown` and `// Scenario: Unknown library`; verify the tests pass
- [ ] 5.3 :feature:settings:impl — entries in `SettingsEntries`: `LicensesKey` as `listPane(sceneKey = LicensesKey, detailPlaceholder = …)`, `LicenseDetailKey` as `detailPane(sceneKey = LicensesKey)` with the assisted factory and `backOrNull(key)`; the list opens a library with `navigator.navigateDetail(LicenseDetailKey(id))`; verify `./gradlew :app:assembleDebug` passes
- [ ] 5.4 :feature:settings:impl — screen tests: list rows show name, version and licence names and a tap reports the id; detail shows holder, licence texts and notices, no `<year> <owner>` line, and the not-found message; phone and tablet goldens for the list, the detail (with a long notice) and the not-found state; verify `./gradlew :feature:settings:impl:testDebugUnitTest` passes

## 6. App integration (:app)

- [ ] 6.1 :app — navigation tests in `AppNavigationTest`: `// Scenario: Licence beside the list` (foldable: Settings → licences → a library shows the list and the licence together, not the home list; opening another library replaces it), `// Scenario: Licence placeholder on a wide window`, `// Scenario: Back to the licences list on a phone`, `// Scenario: Leaving the licences on a wide window` (the list's Back on a foldable with a licence open returns to Settings in one tap); verify `./gradlew :app:testDebugUnitTest` passes
- [ ] 6.2 :app — check the release build: `./gradlew :app:assembleRelease`, install the release APK on an emulator, open Settings → Open-source licences → ML Kit text recognition and LiteRT in airplane mode, and confirm the list, the licence texts and the notices show (`android-qa` journey on a release build as well as debug)

## 7. Gate

- [ ] 7.1 Run `./gradlew spotlessApply && ./gradlew spotlessCheck lint coverageVerify assembleDebug -Proborazzi.test.verify=true` and verify it passes

## Workflow follow-up

- Open the PR with `Fixes #23` in its body and the ticked tasks.md.
- Archive the change with `/opsx:archive` in its own `docs(openspec)` commit or PR after the implementation is merged.
