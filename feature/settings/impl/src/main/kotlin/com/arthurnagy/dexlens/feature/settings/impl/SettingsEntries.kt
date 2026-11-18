package com.arthurnagy.dexlens.feature.settings.impl

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import com.arthurnagy.dexlens.core.navigation.Navigator
import com.arthurnagy.dexlens.core.ui.isDetailShownBesideList
import com.arthurnagy.dexlens.feature.settings.api.ContentOfferKey
import com.arthurnagy.dexlens.feature.settings.api.LicenseDetailKey
import com.arthurnagy.dexlens.feature.settings.api.LicensesKey
import com.arthurnagy.dexlens.feature.settings.api.SettingsKey

/**
 * Settings, the first-launch offer, and the open-source licences: a list-detail scene of its own ([LicensesKey] as its
 * scene key), so the licences list shows beside a licence on wide windows and the home list never joins it.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun EntryProviderScope<NavKey>.settingsEntries(navigator: Navigator) {
    entry<SettingsKey> {
        SettingsScreen(onNavigateBack = navigator::goBack, onOpenLicences = { navigator.navigate(LicensesKey) })
    }
    entry<ContentOfferKey>(metadata = DialogSceneStrategy.dialog()) {
        ContentOfferDialog(onClose = navigator::goBack)
    }
    entry<LicensesKey>(
        metadata = ListDetailSceneStrategy.listPane(sceneKey = LicensesKey, detailPlaceholder = {
            LicencePlaceholder()
        }),
    ) { key ->
        // backFrom: with a licence open beside the list, Back leaves both in one step.
        LicensesScreen(
            onNavigateBack = navigator.backFromOrNull(key),
            onOpenLibrary = { id -> navigator.navigateDetail(LicenseDetailKey(id)) },
        )
    }
    entry<LicenseDetailKey>(metadata = ListDetailSceneStrategy.detailPane(sceneKey = LicensesKey)) { key ->
        // Beside the list, the list's Back is the way out: a second arrow next to it would only clear the licence.
        val besideList = isDetailShownBesideList()
        LicenseDetailScreen(
            viewModel = hiltViewModel<LicenseDetailViewModel, LicenseDetailViewModel.Factory>(creationCallback = { it.create(key) }),
            onNavigateBack = navigator.backOrNull(key).takeUnless { besideList },
        )
    }
}
