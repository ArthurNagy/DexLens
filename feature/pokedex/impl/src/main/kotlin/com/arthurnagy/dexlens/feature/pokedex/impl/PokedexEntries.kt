package com.arthurnagy.dexlens.feature.pokedex.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.arthurnagy.dexlens.core.navigation.Navigator
import com.arthurnagy.dexlens.feature.entry.api.EntryKey
import com.arthurnagy.dexlens.feature.pokedex.api.PokedexKey
import com.arthurnagy.dexlens.feature.settings.api.ContentOfferKey
import com.arthurnagy.dexlens.feature.settings.api.SettingsKey

/** The home list, shown beside its entry on wide windows. Opening a species replaces any entry already showing. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun EntryProviderScope<NavKey>.pokedexEntries(navigator: Navigator) {
    entry<PokedexKey>(
        metadata = ListDetailSceneStrategy.listPane(
            detailPlaceholder = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.pokedex_detail_placeholder), style = MaterialTheme.typography.bodyLarge)
                }
            },
        ),
    ) {
        PokedexScreen(
            onOpenSpecies = { id -> navigator.navigateDetail(EntryKey(id)) },
            onOpenSettings = { navigator.navigate(SettingsKey) },
            onShowOffer = { navigator.navigate(ContentOfferKey) },
            canNavigateBack = navigator.canGoBack,
        )
    }
}
