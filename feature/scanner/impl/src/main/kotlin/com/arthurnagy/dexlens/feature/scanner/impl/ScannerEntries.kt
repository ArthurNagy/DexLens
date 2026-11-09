package com.arthurnagy.dexlens.feature.scanner.impl

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.arthurnagy.dexlens.core.navigation.Navigator
import com.arthurnagy.dexlens.feature.entry.api.EntryKey
import com.arthurnagy.dexlens.feature.scanner.api.ScannerKey

/**
 * The camera screen. Opening a result replaces it with the entry, so the entry always sits in the app's list-detail
 * scene and Back returns to the list.
 */
internal fun EntryProviderScope<NavKey>.scannerEntries(navigator: Navigator) {
    entry<ScannerKey> {
        ScannerScreen(
            onNavigateBack = navigator::goBack,
            onOpenSpecies = { id ->
                navigator.goBack()
                navigator.navigateDetail(EntryKey(id))
            },
        )
    }
}
