# Design

## Context

See proposal.md for the why. Since #22, every `:app` variant carries two generated files:

- `res/raw/aboutlibraries.json`: AboutLibraries' library list for that variant's runtime classpath. Each library has
  a `uniqueId` (`group:artifact`), name, version, developers or organisation, and licences with their full texts. It
  is kept through resource shrinking by `app/src/main/res/raw/keep.xml`. 200 libraries in release.
- `assets/embedded_notices/`: the notices Google embeds in its AARs. `index.json` maps `group:artifact:version` to
  `{ "<file in the AAR>": "<asset path>" }`. The files are `third_party_licenses.txt` (Play services, ML Kit), its
  `third_party_licenses.json` offset index, and LiteRT's `THIRD_PARTY_NOTICE.txt` (about 2 MB).

The BSD-3-Clause and ISC texts are generic SPDX templates whose copyright line reads `Copyright (c) <year> <owner>`
(#23 comment). Settings today is a plain full-screen entry (`SettingsKey`) with an About section of two `Text`s and a
snackbar for `pendingError`. The app's list-detail scene comes from `rememberListDetailSceneStrategy` in
`AppNavigation.kt`, which the home list (`PokedexKey`, `listPane`) and the entry (`EntryKey`, `detailPane`) use.

## Goals / Non-Goals

**Goals:**
- Read the generated files only in `:core:data`, behind `:core:domain` interfaces, as the issue's technical notes and
  #22 decided. The feature sees plain `:core:model` types.
- Keep every read local: resources, assets and `PackageManager`. No `:core:network` involvement.
- Render a 2 MB notice without a frame drop.

**Non-Goals:**
- No ADR: the maintainer chose not to write one for AboutLibraries. The decision and its alternatives are recorded on
  #22 and in the technical design comment on #23. `aboutlibraries-core` in `:core:data` is part of that same decision.
- No search or filtering in the licences list.
- No form factors beyond `:app` on phones, tablets and foldables.

## Decisions

### Types and layering

- `:core:model`: `OpenSourceLibrary(id: String, name: String, version: String?, licenseNames: List<String>)` for the
  list, and `OpenSourceLibraryDetail(library: OpenSourceLibrary, copyrightHolder: String?, licenses:
  List<LibraryLicense>, notices: List<LibraryNotice>)` with `LibraryLicense(name: String, text: String)` and
  `LibraryNotice(fileName: String, text: String)` for the detail (the file name titles the notice's section). The
  list type carries no texts, so the list screen never holds 10 MB of strings. No defaults on any property.
- `:core:domain`: `OpenSourceLibraryRepository` with `suspend fun libraries(): Result<List<OpenSourceLibrary>,
  DomainError>` and `suspend fun library(id: String): Result<OpenSourceLibraryDetail, DomainError>` (`NotFound` for an
  unknown id). `AppInfoRepository` with `suspend fun versionName(): Result<String, DomainError>`. Use cases
  `GetOpenSourceLibrariesUseCase`, `GetOpenSourceLibraryUseCase`, `GetAppVersionUseCase`, each in its own file in a
  new `about` package. No typed errors: `NotFound` and `Unknown` cover every failure.
- `:core:data`, package `about`:
  - `NoticesSource` (internal interface): `librariesJson(): String` and `asset(path: String): String`.
    `AndroidNoticesSource` finds the raw resource by name (`resources.getIdentifier("aboutlibraries", "raw", …)`),
    because `:core:data` can't see `:app`'s `R`. Lint's `DiscouragedApi` is suppressed there with a comment pointing
    at `keep.xml`. It reads assets with `AssetManager`. A missing resource (id 0) throws, which `catchStorage` turns
    into `DomainError.Unknown`.
  - `BundledOpenSourceLibraryRepository` is `@Singleton`, so the list and detail ViewModels share one parse. It parses
    the JSON once with `aboutlibraries-core`'s `Libs.Builder().withJson`, on `DispatcherProvider.io` inside
    `catchStorage`, and caches the parsed `Libs` (a `Mutex`-guarded lazy value). It sorts by name, ignoring case. It reads embedded notices only in `library(id)`: it matches index keys whose
    `group:artifact` equals the library's `uniqueId`, ignores `third_party_licenses.json` (an offset index, not text),
    and returns each other file's text once.
  - `PackageAppInfoRepository` reads `packageManager.getPackageInfo(packageName, 0).versionName` inside
    `catchStorage`. A null `versionName` maps to `DomainError.Unknown`, and Settings then shows no version line.
  - Both are bound in `DataModule`. Fakes `FakeOpenSourceLibraryRepository` and `FakeAppInfoRepository` go in
    `:core:testing`.

Alternative considered: `:app` hands `:core:data` the resource id through Hilt. That needs a public qualifier or
interface in `:core:data` just for one `Int`, and the name lookup is already kept safe by `keep.xml`.

### Copyright holder and template placeholders

The copyright holder is the library's organisation name, otherwise its developers' names joined with commas, otherwise
null. When mapping licence texts, `:core:data` removes a template copyright line: any line that, trimmed, starts with
`Copyright` (case-insensitive) and contains both `<year>` and `<owner>`. That covers both committed forms,
BSD-3-Clause's `Copyright (c) <year> <owner>. ` and ISC's `Copyright <year> <owner> `. The detail screen shows "Copyright: <holder>" above the
texts when the holder is known. Removing the line in the data layer keeps the rule testable without a screen and
covers any future template licence.

Alternative considered: committing a text per library with its real copyright line. That puts a hand-maintained file
per BSD/ISC library back into the build, and would go stale on every bump, which #22 set out to avoid.

### Navigation and layout

- `:feature:settings:api`: `@Serializable data object LicensesKey : NavKey` and `@Serializable data class
  LicenseDetailKey(val libraryId: String) : NavKey`.
- `SettingsEntries`: `LicensesKey` with `ListDetailSceneStrategy.listPane(sceneKey = LicensesKey, detailPlaceholder =
  { … })`, and `LicenseDetailKey` with `detailPane(sceneKey = LicensesKey)` and an assisted `LicenseDetailViewModel` (`hiltViewModel<…, Factory>
  (creationCallback = { it.create(key) })`), the same pattern as `EntryEntries`. Settings opens the list with
  `navigator.navigate(LicensesKey)`, and the list opens a library with `navigator.navigateDetail(LicenseDetailKey(id))`,
  so opening another library replaces the detail. The detail's Back control uses `navigator.backOrNull(key)`.
- The list's Back control uses a new `Navigator.backFrom(key)` in `:core:navigation`: it removes `key` and every entry
  above it, never the root. On a wide window with a library open, the list's Back otherwise resolves to `goBack`,
  which pops only the detail, so the user would need two taps to leave. The home list never hit this because it is
  the root and shows no Back. The system back gesture keeps popping one entry at a time, as everywhere else.
  Alternative considered: accept the double tap. It reads as a broken button on a tablet.
- Back stack `[Pokedex, Settings, Licenses, LicenseDetail]`: the scene strategy stops collecting at the first entry
  without pane metadata (Settings), so the licences list shows beside the licence and the home list does not. Giving
  the licences panes their own `sceneKey` keeps the two scenes apart even if Settings ever becomes a pane. An `:app`
  navigation test pins this on a foldable, next to the existing `foldableShowsBothPanes`.

### Screens and state

- `SettingsState` gains `versionName: String?` and `@StringRes pendingMessage: Int?`, cleared by
  `onMessageConsumed()`. `SettingsViewModel` loads the version once at start. The About section becomes: version
  text, credits text, legal text, then two clickable rows, "Open-source licences" and "Source code", with test tags
  `settings:licenses` and `settings:source`.
- The source link: the screen starts `Intent(ACTION_VIEW, Uri.parse(url))` itself, because starting an activity needs
  a `Context`, which a ViewModel doesn't hold. On `ActivityNotFoundException` it calls
  `viewModel.onSourceLinkUnavailable()`, which sets `pendingMessage`. The existing snackbar host shows it. The URL is a
  `translatable="false"` string.
- `LicensesViewModel` state: `libraries: List<OpenSourceLibrary>`, `isLoading`, `pendingError`. `LicensesScreen`
  is a `LazyColumn` of `ListItem`s (name, then version and licence names), keyed by id.
- `LicenseDetailViewModel` state: `name: String?`, `version: String?`, `rows: List<LicenceRow>`, `isLoading`,
  `notFound: Boolean`, `pendingError: DomainError?`. `LicenceRow` is a sealed type: `Holder(text)`,
  `SectionTitle(text)` (a licence name, or the notice's file name for a bundled notice) and `Paragraph(text)`. The
  ViewModel builds the rows off the main thread (`DispatcherProvider.default`): holder, then per licence its title and
  its paragraphs split on blank lines, then per notice its title and paragraphs. A paragraph longer than 40 lines or
  about 4,000 characters is cut at line breaks into several rows (Eigen's file list in the notices is 700 lines). It keeps only the rows, not the
  `OpenSourceLibraryDetail` it built them from, so no text is held twice. `LicenseDetailScreen` renders the rows in a
  `LazyColumn` with a stable key per row index. One `Text` with a 2 MB notice (LiteRT's, or ML Kit's 2.4 MB
  `third_party_licenses.txt`) would freeze layout. `NotFound` sets `notFound` and the screen shows the "can't be
  found" message in place of the rows. Any other error goes to the snackbar.
- New strings carry the `settings_` prefix and use "licence" for the noun. The credits name "MIT License",
  "BSD-3-Clause" and "Apache-2.0" as written. The model credit reads "Image recognition uses a MobileNetV4 model
  (timm, Apache-2.0), fine-tuned for DexLens."

## Risks / Trade-offs

- [The raw resource is renamed by resource shrinking or obfuscation in release] → `keep.xml` from #22 keeps it, and
  `getIdentifier` resolves the kept name. `android-qa` runs the licences journey on a release build as well as debug.
- [The scene strategy also picks up the home list, showing three panes or the wrong list] → the `:app` navigation test
  on a foldable fails if anything but the licences list and the licence show.
- [A notice file name or index format changes in a later #22 bump] → the repository test reads a fixture in the same
  format, and #22's `OpenSourceNoticesTest` checks the real files.
- [Parsing the 1 MB list JSON on first open takes noticeable time] → it runs on IO once per process, and the list
  shows a loading state meanwhile.
- [The copyright-line pattern removes a real copyright line] → it needs both the literal `<year>` and `<owner>`
  placeholders, which never appear in a real notice. The repository test keeps a real copyright line next to both
  template forms.

## Migration Plan

None: additive screens and strings, no stored data. Rollback is reverting the PR.
