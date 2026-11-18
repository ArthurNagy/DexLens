package com.arthurnagy.dexlens.feature.entry.impl

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.arthurnagy.dexlens.core.navigation.Navigator
import com.arthurnagy.dexlens.core.ui.isDetailShownBesideList
import com.arthurnagy.dexlens.feature.entry.api.EntryKey

/** The full entry, the detail pane of the app's list-detail scene. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun EntryProviderScope<NavKey>.entryEntries(navigator: Navigator) {
    entry<EntryKey>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
        // Right above the home list and beside it, the entry needs no Back arrow: the list is right there. Opened from
        // another entry's evolution line it keeps the arrow, which goes back to the previous species.
        val besideList = isDetailShownBesideList()
        val openedFromAnEntry = navigator.entryBelow(key) is EntryKey
        EntryScreen(
            viewModel = hiltViewModel<EntryViewModel, EntryViewModel.Factory>(creationCallback = { it.create(key) }),
            onNavigateBack = navigator.backOrNull(key),
            showBackArrow = !besideList || openedFromAnEntry,
            onOpenSpecies = { id -> navigator.navigate(EntryKey(id)) },
        )
    }
}
