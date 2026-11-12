# CLAUDE.md

Scaffolded with the [**android-forge**](https://github.com/arthurnagy/android-forge) Claude Code plugin. Plugin docs are the source of truth for architecture, stack, CI, git, product, release, observability, testing, secrets, i18n/a11y and form factors. This file holds only project-specific facts.

android-forge project rules apply here (the plugin's agents and skills check for this line).

## Product
- **Name:** DexLens
- **Application ID / base package:** com.arthurnagy.dexlens
- **minSdk:** 29 · compileSdk / targetSdk: `androidCompileSdk` / `androidTargetSdk` in `gradle/libs.versions.toml`
- **One-liner:** Offline-first Pokémon reference with camera name recognition
- **Spec:** [docs/MVP_SPEC.md](docs/MVP_SPEC.md)
- **Figma:** (none)

## Form factors
- Phones, tablets, foldables — one adaptive `:app`

## Build & Run
```bash
./gradlew assembleDebug                      # debug APK(s)
./gradlew unitTests                          # every module's local tests (Robolectric included)
./gradlew spotlessCheck lint coverageVerify assembleDebug -Proborazzi.test.verify=true   # the CI gate
./gradlew recordRoborazziDebug               # re-record screenshot goldens after an intended UI change
```

## Modules
- :app · :core:{model,domain,data,datastore,designsystem,ui,navigation,testing} · :core:network · build-logic/

- :core:database — Room 3: entities, DAOs, DexDatabase (prebuilt)
- :benchmarks — Baseline + startup profile generator and Macrobenchmarks (device only): `./gradlew :app:generateReleaseBaselineProfile` regenerates app/src/release/generated/baselineProfiles/ — commit it

Features: pokedex (home list, search and camera scanner), entry, settings

## Stack (locked — see plugin [docs/stack.md](https://github.com/arthurnagy/android-forge/blob/main/docs/stack.md))
Jetpack Compose + Material 3 (adaptive) · Hilt · Orbit MVI (state-only) · Navigation 3 (api/impl features, Hilt `@IntoSet` entries) · kotlin-result `Result<T, DomainError>` · DataStore · Coil 3 · Timber · java.time · kotlinx-serialization · Android resources · JUnit 4 + Robolectric + Roborazzi · JaCoCo · Lint + Spotless/ktlint + compose-rules.

Optional in this project: Retrofit 3 + OkHttp 5 (:core:network), Room 3 (:core:database).

## Rules the reviewer enforces (full text in the plugin docs below)
- A feature `impl` depends only on `:core:{domain,model,ui,designsystem,navigation}` and other features' `:api` —
  never `:core:data`, storage/network modules or another `impl`. Nav keys live in `:api`.
- Hilt constructor injection; `@HiltViewModel`; entries contributed with `@IntoSet`. Screens get lambdas, never the Navigator.
- Orbit state-only: no `postSideEffect` — one-shot events are consumable state slots (`pendingNavigation`,
  `pendingMessage`) cleared by `onXConsumed()`.
- `Result<T, DomainError>`; exceptions stop at `catchNetwork { }` / `catchStorage { }`. No `Dispatchers.*` — inject `DispatcherProvider`.
- `internal` by default; a library module's resource keys carry its `resourcePrefix`; no orientation/resizability locks.
- Tests: fakes over mocks, phone + tablet goldens per screen, `// Scenario: <name>` on each acceptance test.

## Work & spec workflow
Work starts as a GitHub issue (forms in `.github/ISSUE_TEMPLATE/`). Open issues labeled `ready`
are the backlog, ordered `priority:high` → unlabeled → `priority:low`. **Only a human applies
`ready`**: Claude may file and refine issues (`/android-forge-refine`) but never approves them.

Behavior changes go through OpenSpec: `/opsx:propose` (naming the issue: `Issue: #<n>`) →
optionally the `android-spec-critic` agent → `/opsx:apply` → a PR whose body says `Fixes #<n>` →
`/opsx:archive` once merged. `openspec/specs/` records what the app does today;
`openspec/config.yaml` holds the spec rules for this project. Direct edits are for non-behavioral
work only (docs, chores, behavior-neutral refactors). The `spec-link` CI check fails a `feat:` PR
that changes production code without an OpenSpec change; the `no-spec` label is the deliberate
escape hatch.

## Project conventions (linked to plugin docs)
- Architecture: https://github.com/arthurnagy/android-forge/blob/main/docs/architecture.md
- Stack: https://github.com/arthurnagy/android-forge/blob/main/docs/stack.md
- Form factors: https://github.com/arthurnagy/android-forge/blob/main/docs/form-factors/
- Android CLI & skills: https://github.com/arthurnagy/android-forge/blob/main/docs/android-cli.md
- Quality tiers: https://github.com/arthurnagy/android-forge/blob/main/docs/quality.md
- Testing: https://github.com/arthurnagy/android-forge/blob/main/docs/testing.md
- Product workflow (issues, OpenSpec, ADRs): https://github.com/arthurnagy/android-forge/blob/main/docs/product-workflow.md
- Git & PRs: https://github.com/arthurnagy/android-forge/blob/main/docs/git-conventions.md
- CI: https://github.com/arthurnagy/android-forge/blob/main/docs/ci.md
- Release: https://github.com/arthurnagy/android-forge/blob/main/docs/release.md
- Observability: https://github.com/arthurnagy/android-forge/blob/main/docs/observability.md
- Secrets: https://github.com/arthurnagy/android-forge/blob/main/docs/secrets.md
- i18n & a11y: https://github.com/arthurnagy/android-forge/blob/main/docs/i18n-a11y.md
- Autonomous build loop (opt-in): https://github.com/arthurnagy/android-forge/blob/main/docs/autoloop.md

## Android CLI and Android skills

Devices go through Google's Android CLI, not adb: `android emulator list|start|stop`,
`android describe` (where each module's APK is), `android install`, `android run [--type TILE|…]`,
`android layout`, `android screen capture`, `android input tap|swipe|text|key|scroll` — adb only
lists devices (`adb devices -l`). Use an emulator for ad-hoc runs; never install to a physical device
without asking first. Before using an unfamiliar or pre-stable Jetpack API, look it up:
`android docs search "<keywords>"` then `android docs fetch kb://…`.

Google's Android skills live in `.claude/skills/` (installed per tier by android-forge — baseline plus
this project's form factors and capabilities): `adaptive`, `navigation-3`, `edge-to-edge`,
`navigation-event`, `testing-setup`, `jetpack-compose-performance`, `android-profiler`, the two security
auditors, `r8-analyzer`, `play-policy-insights`, … They load when the work matches — use them.

Before committing, ask for the `android-reviewer` agent to audit the diff against the rules above. To check a
change against its acceptance scenarios on a device (journeys + screenshots on each form factor it touches), ask
for the `android-qa` agent after `/opsx:apply`.

Prefer android-forge slash commands over hand-rolled scaffolding:
`/android-forge-add-feature`, `/android-forge-add-screen`, `/android-forge-add-form-factor`, `/android-forge-add-library`, `/android-forge-bump-stack`, `/android-forge-spec`, `/android-forge-refine`, `/android-forge-doctor`, `/android-forge-add-autoloop`.

Writing code inside a layer follows the plugin's `domain-layer`, `data-layer` and `presentation-layer` skills
(use cases and errors · repositories and the exception boundary · ViewModels, screens and navigation, each with
the tests the gates expect) — they load when you work in that layer.

## Project-specific overrides

- Atomics: use `kotlin.concurrent.atomics`, never `java.util.concurrent.atomic` ([ADR 0012](docs/DECISIONS/0012-kotlin-atomics.md)).

## Timeline

