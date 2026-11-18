# 15. Make Hilt code generation reliable: KSP 2.3.14 and an aggregated-deps guard

Date: 2026-11-18

## Status

Accepted

## Context

KSP 2.3.13 with Hilt 2.60.1 sometimes produced no Hilt output for a module while its KSP task reported success
([#29](https://github.com/ArthurNagy/DexLens/issues/29)). It happened twice:

- **Locally:** after `clean`, `:feature:pokedex:impl:kspReleaseKotlin` was reported up to date with no outputs. Hilt
  dropped the pokedex entries, R8 stripped the home screen and the release APK crashed on launch. A `--rerun` build
  fixed it. A task can't be UP-TO-DATE after `clean`, so the empty output most likely came FROM-CACHE, stored in the
  build cache by an earlier bad run.
- **On CI** (PR #28, run 44278387029, a docs-only PR): `:app:hiltJavaCompileDebug` failed with
  `[Dagger/MissingBinding] ContentRemoteDataSource cannot be provided`. `ContentNetworkModule` had not changed in
  weeks, and a plain re-run on the same Gradle cache passed.

Likely cause: [google/ksp#3286](https://github.com/google/ksp/issues/3286), "KSP 2.3.13 regression: library symbols
disappear during concurrent in-process executions". When several KSP runs execute at once in one Gradle daemon, one
run clears shared `CoreJarFileSystem` handlers another is still reading. Hilt can't process the classes whose types no
longer resolve, and `ksp<Variant>Kotlin` succeeds with its `hilt_aggregated_deps` output empty or partial. The issue's
investigation found upstream reports matching ours: Hilt 2.60.1, Kotlin 2.4.2x, `[Dagger/MissingBinding]` for a whole
module, a different module each run. 2.3.13 has been pinned since the scaffold; 2.3.14 (2026-11-13) fixes the race.

Alternatives considered:
- **Downgrade to KSP 2.3.12**: also avoids the regression, but goes backwards when 2.3.14 is the upstream fix.
- **`ksp.incremental=false`**: the bug is in concurrent runs, not incremental processing; slower, race remains.
- **Turn off parallel builds**: makes concurrent KSP runs rarer, slows every build, leaves the broken KSP in place.
- **Turn off the build cache or configuration cache**: the caches only replay an output a bad run produced; they
  don't cause it.
- **Bump Kotlin or Hilt too**: not needed, and changing one thing at a time keeps this evidence clean.

## Decision

Use KSP 2.3.14 (`ksp` in `gradle/libs.versions.toml`), with no Kotlin, AGP or Hilt change. The fix needs no
`gradle.properties` change.

The bump changes every KSP task's build-cache key, so outputs a bad 2.3.13 run stored can't be restored and no cache
purge is needed. For `:core:network:kspReleaseKotlin` the key went from `af3e9c35c70f588b6d3a0c47f313015d` (2.3.13) to
`9666d8e27d9f4b1c8d19a5b95b6d7fe8` (2.3.14); three inputs differ: the task implementation (`KspAATask`),
`kspClasspath` (the `symbol-processing-*` jars) and `commandLineArgumentProviders.$0`.

Guard against a recurrence: the `android-forge.hilt` convention registers `check<Variant>HiltAggregatedDeps`
([`HiltAggregatedDepsCheck`](../../build-logic/src/main/kotlin/HiltAggregatedDepsCheck.kt)) for every variant of
every Hilt module, and the variant's `compile<Variant>JavaWithJavac` depends on it. It scans the variant's static
source directories for classes annotated `@InstallIn`, `@HiltViewModel`, `@AndroidEntryPoint`, `@HiltAndroidApp` or
`@HiltWorker`, and requires each one's `hilt_aggregated_deps` entry in the KSP task's outputs. On a miss it fails,
naming the module, the variant and the missing classes. It is its own task with no outputs, so it runs even when the
KSP task is UP-TO-DATE or FROM-CACHE, and it reads only file inputs (configuration-cache safe).

## Consequences

- Easier: a dropped Hilt module fails the build at the module that lost it, instead of shipping an app with missing
  bindings or screens.
- Harder: the guard scans source text, so an annotation spelled through an import alias goes unchecked (never a false
  failure). If Hilt renames its aggregated-deps files, or KSP renames its task or output layout, the guard fails every
  build, loudly, until `HiltAggregatedDepsCheck` or the convention is updated. One green run doesn't prove an
  intermittent bug fixed; the guard is what catches a recurrence.
- Enforced (copied to CLAUDE.md § Project-specific overrides): keep KSP at 2.3.14 or later, and never skip or unwire
  `check<Variant>HiltAggregatedDeps`.
