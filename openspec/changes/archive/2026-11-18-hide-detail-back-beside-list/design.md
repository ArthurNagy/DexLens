# Design

## Context

See proposal.md for the problem. What the code does today:

- `app/.../AppNavigation.kt` builds one `rememberListDetailSceneStrategy<NavKey>(directive =
  calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(adaptiveInfo))` with the default
  `shouldHandleSinglePaneLayout = false`, after `DialogSceneStrategy`. It also computes a separate `sideBySide`
  width check, used only to turn shared-element transitions off on wide windows.
- `app/src/main/AndroidManifest.xml` declares no `android:configChanges` on the activity, so a fold, unfold or window
  resize recreates the activity. `rememberNavBackStack` restores the back stack (and the per-entry saveable state and
  ViewModel stores come back with it), then the scene strategy lays the restored stack out for the new window size.
- `feature/settings/impl/.../SettingsEntries.kt`: `LicensesKey` is `listPane(sceneKey = LicensesKey, …)`,
  `LicenseDetailKey` is `detailPane(sceneKey = LicensesKey)` and gets `onNavigateBack = navigator.backOrNull(key)`.
  `LicenseDetailContent` already hides its arrow (`licence:back`) when `onNavigateBack` is null and uses the lambda for
  nothing else.
- `feature/entry/impl/.../EntryEntries.kt`: `EntryKey` is `detailPane()` (default scene, beside `PokedexKey`'s
  `listPane()`) and gets `onNavigateBack = navigator.backOrNull(key)`. `EntryContent` uses that lambda twice: the
  top-bar arrow (`entry:back`) and the Back action of the `EntryState.Failed` message ("Unknown species: an error
  message is shown with a way back").
- `core/navigation/.../Navigator.kt` is a plain (non-Compose) Android library over the back stack. `backOrNull(key)`
  only knows whether `key` is the root; it cannot know how the scene lays the entries out.
- `material3-adaptive` / `adaptive-navigation3` 1.3.0 (`gradle/libs.versions.toml`). Feature impls get
  `adaptive-navigation3` from the `android-forge.feature.impl` convention plugin; `:core:ui` does not have it yet.

How the library decides the layout (read from the 1.3.0 sources of `ListDetailSceneStrategy` and
`ThreePaneScaffoldScene`):

- `calculateScene` returns a `ThreePaneScaffoldScene` only when the scaffold value has two or more panes showing
  (`paneCount > 1`), unless `shouldHandleSinglePaneLayout` is true. With one pane it returns null and `NavDisplay`
  draws the entry in its plain single-pane scene.
- Inside `ThreePaneScaffoldScene`, every pane (list, detail, extra, and the detail placeholder) is wrapped in
  `CompositionLocalProvider(LocalListDetailSceneScope provides ListDetailSceneScopeImpl(this))`. The public,
  `@ExperimentalMaterial3AdaptiveApi` `LocalListDetailSceneScope` is documented as "If null, this means that
  `ListDetailSceneStrategy` is not the chosen strategy to display the current content". Its
  `scaffoldTransitionScope.scaffoldStateTransition` is the scaffold's `Transition<ThreePaneScaffoldValue>`, whose
  `targetState[ListDetailPaneScaffoldRole.List]` is `PaneAdaptedValue.Hidden` or not.
- The adaptive skill (`.claude/skills/adaptive/SKILL.md`, list-detail considerations) states "Detail screens must not
  show a back arrow when on a list-detail layout". The Nav 3 custom-scene recipe
  (`navigation-3/.../recipes/scenes-listdetail.md`) solves the same problem with a scene-provided
  `LocalBackButtonVisibility`; for the Material strategy the scene already provides `LocalListDetailSceneScope`.

## Goals / Non-Goals

**Goals:**
- One shared signal, read from the scene the strategy actually drew, so the arrow can never disagree with the layout,
  including after a fold, unfold or window resize.
- Each detail entry decides at its entry builder; screens keep taking plain values and lambdas, never the Navigator or
  a scene type, so their tests and goldens stay simple.

**Non-Goals:**
- No change to `Navigator`, to `onBack` / predictive back, to `BackNavigationBehavior`, or to the `sideBySide` check
  that drives shared-element transitions.
- No change to `AppNavigation`'s strategy or directive, and no `configChanges` in the manifest.

## Decisions

### 1. Read `LocalListDetailSceneScope` in a `:core:ui` helper

```kotlin
// :core:ui — ListDetailPanes.kt
/**
 * For detail-pane content only: whether this detail shows beside its list in the list-detail scene the
 * ListDetailSceneStrategy drew (never a window-width check). In the list pane or the detail placeholder it also
 * returns true, so don't call it there.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun isDetailShownBesideList(): Boolean {
    val scope = LocalListDetailSceneScope.current ?: return false
    return scope.scaffoldTransitionScope.scaffoldStateTransition.targetState[ListDetailPaneScaffoldRole.List] !=
        PaneAdaptedValue.Hidden
}
```

- Null scope: the strategy did not draw a two-pane scene (a phone in portrait, or any narrow window), so the detail
  fills the window on its own → `false` → the arrow shows.
- Non-null scope: the content is inside the list-detail scaffold; a detail is "beside the list" when the scaffold's
  target value keeps the list pane visible. The extra check keeps the helper right if the app ever sets
  `shouldHandleSinglePaneLayout = true` (the scaffold would then also draw single-pane layouts, with the list Hidden).
- The name says "detail" because the scope is provided to every pane: called from the list pane or the placeholder it
  would also return true. Only detail entries call it.
- `targetState` rather than `currentState`, so the arrow matches where the layout is going as soon as it changes.
- On a fold, unfold or resize the activity is recreated (no `configChanges`): the restored back stack is laid out
  again for the new size, and the detail entry composes fresh inside whichever scene the strategy picks, so it reads
  the new value on its first composition. The back stack is the same restored stack, so the open library or species
  stays the same. The helper is still a plain composition-local read, so it would also follow a scene change without
  recreation (for example if `configChanges` were added later); the `:core:ui` test covers that reactivity.

Placement: `:core:ui`, because it is shared Compose UI used by two features and `:core:navigation` has no Compose. It
is public (feature modules call it) with KDoc. `:core:ui` adds `implementation(libs.androidx.compose.material3.adaptive.navigation3)`,
a catalog entry `:app` and every feature impl already use, so no new library and no ADR.

Alternatives considered:
- **A window-width check** (`currentWindowAdaptiveInfo()` ≥ 600dp, like `sideBySide`): rejected by the issue. It
  duplicates the strategy's directive and goes wrong whenever they differ (directive tweaks, a detail without its
  list in the scene, `shouldHandleSinglePaneLayout`).
- **Scene type check in `:app`** (wrap `NavDisplay`'s scene and provide our own local, like the recipe's
  `LocalBackButtonVisibility`): needs a custom `SceneStrategy` wrapper or a `SceneDecoratorStrategy` that re-derives
  what `LocalListDetailSceneScope` already gives; more code, same signal.
- **`Navigator.backOrNull` deciding**: the Navigator sees only the back stack, not the layout, and is not Compose.
- **Two private copies in the features**: the issue asks for one shared helper.

### 2. Wire it at each entry builder

The composable call is hoisted into a `val` at the top of the entry content, never made inside a lambda:

- `SettingsEntries`, `LicenseDetailKey`: `val besideList = isDetailShownBesideList()`, then
  `onNavigateBack = navigator.backOrNull(key).takeUnless { besideList }`. `LicenseDetailScreen` /
  `LicenseDetailContent` stay as they are: a null lambda already hides `licence:back`.
- `EntryEntries`, `EntryKey`: `val besideList = isDetailShownBesideList()` and
  `val openedFromAnEntry = navigator.entryBelow(key) is EntryKey`, then a new
  `showBackArrow = !besideList || openedFromAnEntry` to `EntryScreen`, which passes it to `EntryContent`.
  `EntryContent` draws `entry:back` only when `showBackArrow && onNavigateBack != null`; the `Failed` message keeps
  using `onNavigateBack` for its Back action. The issue asks only for the top-bar arrow, and the "Unknown species"
  scenario promises a way back, which stays on every window size.
- Evolution-line entries: tapping a species in the evolution line pushes a new `EntryKey` on top of the current one
  (`navigator.navigate`, not `navigateDetail`). Beside the list, hiding its arrow would leave no on-screen way back to
  the previous species; the list's rows only replace the top entry. So the arrow is hidden only when the entry sits
  directly above its list; when the entry below it is another `EntryKey`, the arrow stays and goes back to that species.
  The question "what is below me?" is a small `Navigator.entryBelow(key)` query in `:core:navigation` (the entry
  builder already holds the Navigator; screens still get only a Boolean and a lambda). Licences have no such chain:
  `navigateDetail` always replaces a licence on top, so a licence is always directly above its list.

Alternative for the evolution case: hide the arrow anyway and rely on system Back. Rejected: system Back is invisible,
and the reviewer found the missing way back a usability regression. Alternative placement: compute it inside
`EntryEntries` from the back stack directly. Rejected: entry builders don't see the back stack, only the Navigator.

Alternative for the entry: pass `onNavigateBack = null` beside the list, like the licence. Rejected: it would also
remove the failed message's Back action on wide windows, a behavior change outside the issue.

### 3. Tests

- Helper reactivity: a Robolectric test in `:core:ui` draws a real `NavDisplay` with
  `rememberListDetailSceneStrategy(directive = …)` where the directive comes from a `mutableStateOf` (two horizontal
  partitions, then one, then two). The detail stand-in shows a marker only when `!isDetailShownBesideList()` and shows
  its key's id. It proves the signal follows the scene the strategy picks, in both scene shapes the app uses. Needs
  `testImplementation(libs.androidx.navigation3.ui)` in `:core:ui`.
- Window size change as the app sees it: `:app` `AppNavigationTest` opens a licence or an entry at
  `w841dp-h701dp-xxhdpi`, then `RuntimeEnvironment.setQualifiers(…)` to a phone portrait size followed by
  `composeRule.activityRule.scenario.recreate()`, and back again, checking the back marker appears and disappears and
  the same id stays shown. This is the path a real fold takes (recreation plus a restored back stack). The compose rule
  only sets content on the first activity, so the test registers an `Application.ActivityLifecycleCallbacks` that sets
  the same `AppNavigation` content on each recreated activity (`onActivityCreated` with a saved state); it keeps the
  Application it registered with and unregisters there in `@After`, or before registering again.
- Evolution line: `AppNavigationTest` on a foldable opens species 1 from the list, pushes species 2 on top (as the
  evolution line does), checks species 2 shows its arrow beside the list and that tapping it shows species 1 with no
  arrow. `NavigatorTest` covers `entryBelow`.
- Wiring (wide and phone scenarios): `AppNavigationTest` drives the real `AppNavigation` at `w841dp-h701dp-xxhdpi`
  (foldable), `MediumTablet` and the default phone portrait size, with stand-in entries that copy the real entries'
  metadata and call `isDetailShownBesideList()` the same way (the existing `showLicencesFeature()` stand-ins, plus an
  entry stand-in over `PokedexKey`/`EntryKey`). The stand-ins don't run the real `SettingsEntries` / `EntryEntries`
  call sites, so the PR asks the reviewer to check those by eye.
- Screens: `LicensesScreenTest` and `EntryScreenTest` check the arrow is hidden when told; the tablet and foldable
  goldens render the detail as it shows beside the list (no arrow) and are re-recorded. Phone goldens keep the arrow
  and must not change.
- On device, `android-qa` runs the journeys on a phone (portrait and landscape), a tablet and a foldable emulator
  (fold/unfold).

## Risks / Trade-offs

- [`LocalListDetailSceneScope` and `scaffoldStateTransition` are `@ExperimentalMaterial3AdaptiveApi`] → the rest of
  the app's list-detail wiring already opts in; the helper is the one place to update if the API moves, and the
  `:core:ui` test fails loudly if the signal changes meaning on a version bump.
- [The helper also returns true in the list pane and the placeholder] → its name and KDoc limit it to detail-pane
  content; only the two detail entry builders call it.
- [The helper reports "beside the list" from the scaffold value, not from whether a list entry exists in the scene] →
  every detail in the app is pushed above its list (`navigateDetail` from the list, `EntryKey` from the evolution line
  above other entries), so a list entry is always present; a detail pushed without its list would already show an
  empty list pane today, which is a separate layout bug.
- [A phone in landscape (about 915dp wide) passes the 600dp breakpoint and shows two panes] → that is the strategy's
  existing layout, not changed here; the arrow follows it (hidden beside the list), which is why the scenarios say
  "phone in portrait".
- [Robolectric recreation in `AppNavigationTest`: the compose rule does not re-run its content on a recreated
  activity] → an Application activity-lifecycle callback sets the same content on each recreated activity, so
  recreation re-composes and `rememberNavBackStack` restores the stack, with no test activity or manifest change. The
  Navigator a stand-in returns belongs to the first activity, so tests never navigate through it after a resize.
- [Evolution-line entries keep their arrow beside the list] → deliberate: it is the only on-screen way back to the
  previous species. Back from there goes to the previous species, and that entry, now directly above the list, hides
  its arrow again.
- [Goldens] → tablet/foldable goldens of both detail screens change on purpose; reviewers compare them to confirm only
  the arrow went.
