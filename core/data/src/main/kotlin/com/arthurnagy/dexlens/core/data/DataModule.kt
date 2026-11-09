package com.arthurnagy.dexlens.core.data

import com.arthurnagy.dexlens.core.domain.DispatcherProvider
import com.arthurnagy.dexlens.core.domain.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds the :core:domain repository interfaces to their `internal` implementations here. The rest
 * of the app depends on the interface, never the implementation (docs/architecture.md § Visibility).
 * One repository per domain type — never an AppRepository god object.
 */
@Module
@InstallIn(SingletonComponent::class)
internal interface DataModule {
    @Binds
    fun bindsDispatcherProvider(impl: RealDispatcherProvider): DispatcherProvider

    @Binds
    fun bindsUserPreferencesRepository(impl: OfflineUserPreferencesRepository): UserPreferencesRepository
}
