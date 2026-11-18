package com.arthurnagy.dexlens.core.ui

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation3.LocalListDetailSceneScope
import androidx.compose.runtime.Composable

/**
 * For detail-pane content only: whether this detail shows beside its list, so it can hide its own Back arrow there.
 *
 * Read from the list-detail scene `ListDetailSceneStrategy` actually drew, never from the window width, so it always
 * agrees with the layout: false when the detail fills the window on its own (a phone in portrait, any narrow window),
 * true when the list pane shows next to it. The scene provides its scope to every pane, so in the list pane or the
 * detail placeholder this also returns true: don't call it there.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun isDetailShownBesideList(): Boolean {
    val scope = LocalListDetailSceneScope.current ?: return false
    return scope.scaffoldTransitionScope.scaffoldStateTransition.targetState[ListDetailPaneScaffoldRole.List] !=
        PaneAdaptedValue.Hidden
}
