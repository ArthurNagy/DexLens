# 15. Make Hilt code generation reliable: KSP 2.3.14 and an aggregated-deps guard

Date: 2026-11-18

## Status

Accepted

## Context

KSP 2.3.13 with Hilt 2.60.1 sometimes produced no Hilt output for a module while its KSP task reported success
([#29](https://github.com/ArthurNagy/DexLens/issues/29)). It happened twice:

- **Locally:** after `clean`, `:feature:pokedex:impl:kspReleaseKotlin` was reported up to date with no outputs. Hilt
  dropped the pokedex entries, R8 stripped the home screen and the release APK crashed on launch. A `--rerun` build
  fixed it. A task can't be UP-TO-DATE after `clean`, so the empty output most likely came FROM-CACHE: stored in the
  build cache by an earlier bad run.
- **On CI** (PR #28, run 44278387029, a docs-only PR): `:app:hiltJavaCompileDebug` failed with
  `[Dagger/MissingBinding] ContentRemoteDataSource cannot be provided`. `ContentNetworkModule` had not changed in weeks,
  and a plain re-run on the same Gradle cache passed.

Likely cause: [google/ksp#3286](https://github.com/google/ksp/issues/3286), "KSP 2.3.13 regression: library symbols
disappear during concurrent in-process executions". When several KSP runs execute at once in one Gradle daemon, one
run clears the shared `CoreJarFileSystem` handlers another is still reading, so library symbols vanish. Hilt can't
process the classes whose types no longer resolve, and `ksp<Variant>Kotlin` succeeds with its `hilt_aggregated_deps` output empty or
partial. The upstream reports match ours (per the issue's investigation): Hilt 2.60.1, Kotlin 2.4.2x, `[Dagger/MissingBinding]` for a whole module, a
different module each run. KSP 2.3.13 has been pinned since the scaffold (2026-11-09). KSP 2.3.14 (2026-11-13) fixes
it by clearing those handlers only when no run is active.

The maintainer confirmed the `gradle.properties` flags (`ksp.*`, `kotlin.incremental*`, `org.gradle.parallel`,
`org.gradle.caching`, `org.gradle.configuration-cache*`). TODO(maintainer): list the exact keys and values here.

Alternatives considered:
- **Downgrade to KSP 2.3.12** — also avoids the regression, but goes backwards when 2.3.14 is the upstream fix.
- **`ksp.incremental=false`** — the bug is in concurrent runs, not incremental processing; every run would get
  slower and the race would remain.
- **Turn off parallel builds (`org.gradle.parallel=false`)** — would make concurrent KSP runs rarer, but slows every
  build and still leaves a broken KSP release in place.
- **Turn off the build cache or configuration cache, locally or on CI** — the caches only replay an output a bad run
  produced; they don't cause it. The issue rules out disabling them project-wide without evidence that they do.
- **Bump Kotlin or Hilt too** — not needed for the fix, and changing one thing at a time keeps this evidence clean.

## Decision

Use KSP 2.3.14 (`ksp` in `gradle/libs.versions.toml`); no Kotlin, AGP or Hilt change and no `gradle.properties`
change.

Guard against any recurrence in build-logic: the `android-forge.hilt` convention registers
`check<Variant>HiltAggregatedDeps` (`HiltAggregatedDepsCheck`) for every variant of every Hilt module, and the
variant's `compile<Variant>JavaWithJavac` depends on it. It scans the module's `src/main` Kotlin sources for classes
annotated `@InstallIn`, `@HiltViewModel`, `@AndroidEntryPoint`, `@HiltAndroidApp` or `@HiltWorker` and requires each to
have a file in `build/generated/ksp/<variant>/java/hilt_aggregated_deps/` named `_<package_with_underscores>_<rest>`,
where `<rest>` is the class name, optionally after `HiltWrapper_` (Hilt's name for non-public, e.g. Kotlin
`internal`, modules) and followed by `_…` (`_HiltModules_BindsModule`, `_GeneratedInjector`, `_HiltModule`). On a
miss it fails with the module, the variant and the missing classes. It is a separate task with no outputs, so it runs
even when the KSP task is UP-TO-DATE or FROM-CACHE, and it reads only its file inputs (configuration-cache safe).

The bump changes every KSP task's build-cache key (the task implementation, `kspClasspath` and the argument provider
all change), so outputs stored by a bad 2.3.13 run can't be restored; no cache purge is needed.

## Consequences

- Easier: a dropped Hilt module now fails the build at the module that lost it, naming the classes, instead of
  shipping an app with missing bindings or screens.
- Harder: the guard finds classes by scanning source text, so an annotation spelled through an import alias goes
  unchecked (it never fails falsely). If Hilt renames its aggregated-deps files, the guard fails every build until
  `HiltAggregatedDepsCheck` is updated. An intermittent bug isn't proven fixed by one green run; the guard is what
  catches a recurrence.
- Enforced (copied to CLAUDE.md § Project-specific overrides): keep KSP at 2.3.14 or later, and never skip or unwire
  `check<Variant>HiltAggregatedDeps`.
