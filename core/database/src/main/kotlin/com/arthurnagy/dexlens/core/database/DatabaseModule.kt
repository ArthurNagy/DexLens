package com.arthurnagy.dexlens.core.database

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun provideDexDatabase(
        @ApplicationContext context: Context,
    ): DexDatabase = Room
        .databaseBuilder<DexDatabase>(context, DEX_DATABASE_NAME)
        .createFromAsset(DEX_DATABASE_ASSET)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO) // Room's own I/O context — the one allowed Dispatchers.* outside RealDispatcherProvider
        .fallbackToDestructiveMigration(dropAllTables = true) // Read-only data: a new app version recopies the asset.
        .build()

    @Provides
    fun provideSpeciesDao(database: DexDatabase): SpeciesDao = database.speciesDao()

    @Provides
    fun provideTypeChartDao(database: DexDatabase): TypeChartDao = database.typeChartDao()

    @Provides
    fun provideGameVersionDao(database: DexDatabase): GameVersionDao = database.gameVersionDao()

    @Provides
    @Singleton
    fun provideContentDatabase(
        @ApplicationContext context: Context,
    ): ContentDatabase = Room
        .databaseBuilder<ContentDatabase>(context, "content.db")
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO) // Room's own I/O context — the one allowed Dispatchers.* outside RealDispatcherProvider
        .build()

    @Provides
    fun provideContentDao(database: ContentDatabase): ContentDao = database.contentDao()

    private const val DEX_DATABASE_NAME = "dexlens.db"
    private const val DEX_DATABASE_ASSET = "databases/dexlens.db"
}
