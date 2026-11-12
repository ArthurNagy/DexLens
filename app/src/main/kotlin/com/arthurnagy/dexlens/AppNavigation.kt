package com.arthurnagy.dexlens

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import com.arthurnagy.dexlens.core.navigation.EntryProviderInstaller
import com.arthurnagy.dexlens.core.navigation.Navigator
import com.arthurnagy.dexlens.core.ui.LocalSharedTransitionScope
import com.arthurnagy.dexlens.feature.pokedex.api.PokedexKey

/**
 * The app's single back stack. Features never reference each other's screens: each impl module
 * contributes its entries through an [EntryProviderInstaller] (Hilt `@IntoSet`), and navigates with
 * other features' keys through the [Navigator].
 *
 * The list-detail scene strategy makes the app adaptive by default: entries a feature marks with
 * `ListDetailSceneStrategy.listPane()` / `detailPane()` metadata show side by side on wide windows
 * (from 600dp: tablets, unfolded foldables, desktop) and one at a time on phones. (The material3-adaptive scene
 * strategies are still `@ExperimentalMaterial3AdaptiveApi`.)
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalSharedTransitionApi::class, ExperimentalComposeUiApi::class)
@Composable
internal fun AppNavigation(
    entryProviderInstallers: Set<EntryProviderInstaller>,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(PokedexKey)
    val navigator = remember(backStack) { Navigator(backStack) }

    val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val sideBySide = adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    val listDetailStrategy =
        rememberListDetailSceneStrategy<NavKey>(directive = calculatePaneScaffoldDirectiveWithTwoPanesOnMediumWidth(adaptiveInfo))
    // Test tags become resource IDs, so the Baseline Profile and benchmark journeys (UI Automator) can find them.
    SharedTransitionLayout(modifier = modifier.semantics { testTagsAsResourceId = true }) {
        CompositionLocalProvider(LocalSharedTransitionScope provides if (sideBySide) null else this) {
            NavDisplay<NavKey>(
                backStack = backStack,
                sharedTransitionScope = this@SharedTransitionLayout,
                onBack = navigator::goBack,
                // Saveable state + a ViewModelStore per entry: a screen's hiltViewModel() is scoped to its
                // entry and cleared when that entry is popped.
                entryDecorators =
                    listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                sceneStrategies = listOf(dialogStrategy, listDetailStrategy),
                popTransitionSpec = { popTransition() },
                // The default predictive pop shrinks the leaving screen while a shared element flies out of it. The
                // back arrow's fade, scrubbed by the gesture, keeps the entry whole as its art returns to the list.
                predictivePopTransitionSpec = { popTransition() },
                entryProvider =
                    entryProvider {
                        entryProviderInstallers.forEach { install -> install(navigator) }
                    },
            )
        }
    }
}

/** One fade for leaving a screen, whether by the back arrow or the back gesture. */
internal fun popTransition(): ContentTransform = ContentTransform(fadeIn(tween(POP_DURATION_MILLIS)), fadeOut(tween(POP_DURATION_MILLIS)))

private const val POP_DURATION_MILLIS = 700
