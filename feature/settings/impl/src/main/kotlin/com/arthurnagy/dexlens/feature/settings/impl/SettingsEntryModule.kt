package com.arthurnagy.dexlens.feature.settings.impl

import com.arthurnagy.dexlens.core.navigation.EntryProviderInstaller
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Contributes this feature's entries to the app's `Set<EntryProviderInstaller>` (Hilt multibinding).
 * :app picks it up just by depending on :feature:settings:impl — no app code lists features.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object SettingsEntryModule {
    @Provides
    @IntoSet
    fun provideSettingsEntries(): EntryProviderInstaller = { navigator -> settingsEntries(navigator) }
}
