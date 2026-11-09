package com.arthurnagy.dexlens.feature.entry.impl

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.arthurnagy.dexlens.core.navigation.Navigator
import com.arthurnagy.dexlens.feature.entry.api.EntryKey

/** The full entry, the detail pane of the app's list-detail scene. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun EntryProviderScope<NavKey>.entryEntries(navigator: Navigator) {
    entry<EntryKey>(metadata = ListDetailSceneStrategy.detailPane()) { key ->
        EntryScreen(
            viewModel = hiltViewModel<EntryViewModel, EntryViewModel.Factory>(creationCallback = { it.create(key) }),
            onNavigateBack = navigator.backOrNull(key),
            onOpenSpecies = { id -> navigator.navigate(EntryKey(id)) },
        )
    }
}
