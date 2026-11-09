package com.arthurnagy.dexlens.core.data

import android.content.Context
import androidx.work.WorkManager
import com.arthurnagy.dexlens.core.data.content.ImageTranscoder
import com.arthurnagy.dexlens.core.data.content.OfflineContentPackRepository
import com.arthurnagy.dexlens.core.data.content.OfflineFlavorTextRepository
import com.arthurnagy.dexlens.core.data.content.WebpTranscoder
import com.arthurnagy.dexlens.core.data.species.DeviceDisplayLanguage
import com.arthurnagy.dexlens.core.data.species.DisplayLanguage
import com.arthurnagy.dexlens.core.data.species.OfflineSpeciesRepository
import com.arthurnagy.dexlens.core.data.species.OfflineTypeChartRepository
import com.arthurnagy.dexlens.core.domain.DispatcherProvider
import com.arthurnagy.dexlens.core.domain.UserPreferencesRepository
import com.arthurnagy.dexlens.core.domain.content.ContentPackRepository
import com.arthurnagy.dexlens.core.domain.content.FlavorTextRepository
import com.arthurnagy.dexlens.core.domain.species.SpeciesRepository
import com.arthurnagy.dexlens.core.domain.species.TypeChartRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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

    @Binds
    fun bindsSpeciesRepository(impl: OfflineSpeciesRepository): SpeciesRepository

    @Binds
    fun bindsTypeChartRepository(impl: OfflineTypeChartRepository): TypeChartRepository

    @Binds
    fun bindsDisplayLanguage(impl: DeviceDisplayLanguage): DisplayLanguage

    @Binds
    fun bindsContentPackRepository(impl: OfflineContentPackRepository): ContentPackRepository

    @Binds
    fun bindsFlavorTextRepository(impl: OfflineFlavorTextRepository): FlavorTextRepository

    @Binds
    fun bindsImageTranscoder(impl: WebpTranscoder): ImageTranscoder

    companion object {
        @Provides
        @Singleton
        fun provideWorkManager(
            @ApplicationContext context: Context,
        ): WorkManager = WorkManager.getInstance(context)
    }
}
