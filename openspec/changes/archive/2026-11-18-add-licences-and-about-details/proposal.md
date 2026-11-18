# Proposal

Issue: #23

## Why

DexLens ships Apache-2.0 and other open-source libraries whose licences require their notices to ship with the app, but
the app has no licences screen. Settings > About credits only PokeAPI: it doesn't credit ML Kit, LiteRT or the bundled
MobileNetV4 model, doesn't show the app version and doesn't link to the source code, even though the app is published
as an open-source showcase. #22 now generates the library list and Google's embedded notices at build time, so the
first public release (v0.1.0) can show them, offline like everything else.

## What Changes

- Settings > About shows the installed app version, credits PokeAPI (BSD-3-Clause), ML Kit text recognition, LiteRT
  and the MobileNetV4 image model (timm, Apache-2.0, fine-tuned for DexLens), says DexLens is open source under the
  MIT License, and keeps the unofficial fan-project notice.
- A new "Open-source licences" row opens a screen listing every third-party library bundled in the APK with its
  licence names. Opening a library shows its copyright holder, its full licence texts and any notices its artifact
  embeds, all from the APK with no network request. On wide windows the list and a library's licence show side by side.
- A new "Source code" row opens https://github.com/ArthurNagy/DexLens in the device's browser or app chooser. When no
  app can open it, a short message is shown instead.
- New user-facing text uses the UK spelling "licence" for the noun; proper names keep theirs ("MIT License").

## Non-goals

- No in-app web view, and no downloading of licence texts or library metadata at runtime
- No changes to the artwork download, the scanner switch or theme settings
- No changelog or "what's new" screen
- No new top-level destination on the home screen. The licences screen is reached from Settings only
- No runtime licence UI library (`aboutlibraries-compose-*`, `play-services-oss-licenses`): the app's own Compose
  screens over `:core:domain` use cases
- No new feature module: the licence screens live in `:feature:settings`, and the impl never reads the generated
  resource or asset directly
- No in-app copy of the build tooling: library data comes only from what #22 generates

## Capabilities

### New Capabilities
- `open-source-notices`: the in-app list of bundled third-party libraries and each library's licence and notice texts,
  readable offline.

### Modified Capabilities
- `app-settings`: the "Credits shown" scenario widens to the full credits; Settings gains the app version, an entry to
  the open-source licences and a link to the source code.

## Impact

- `:core:model`: new `OpenSourceLibrary`, `OpenSourceLibraryDetail`, `LibraryLicense` types.
- `:core:navigation`: a `Navigator` action that leaves a list-detail list in one step (see design.md).
- `:core:domain`: new `OpenSourceLibraryRepository`, `AppInfoRepository` and three use cases.
- `:core:data`: implementations reading `raw/aboutlibraries` (through `aboutlibraries-core`, a new dependency of
  this module) and `assets/embedded_notices/` generated in `:app` by #22, and the version from `PackageManager`.
- `:core:testing`: fakes for the two repositories.
- `:feature:settings:api`: `LicensesKey`, `LicenseDetailKey`. `:feature:settings:impl`: two screens, two ViewModels,
  Settings state and About section changes, strings, tests and goldens.
- No new permissions, no network use, no change to the release workflow. Phones, tablets and foldables only.
