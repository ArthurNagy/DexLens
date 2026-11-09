package com.arthurnagy.dexlens.feature.settings.impl

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import com.arthurnagy.dexlens.core.navigation.Navigator
import com.arthurnagy.dexlens.feature.settings.api.ContentOfferKey
import com.arthurnagy.dexlens.feature.settings.api.SettingsKey

internal fun EntryProviderScope<NavKey>.settingsEntries(navigator: Navigator) {
    entry<SettingsKey> {
        SettingsScreen(onNavigateBack = navigator::goBack)
    }
    entry<ContentOfferKey>(metadata = DialogSceneStrategy.dialog()) {
        ContentOfferDialog(onClose = navigator::goBack)
    }
}
