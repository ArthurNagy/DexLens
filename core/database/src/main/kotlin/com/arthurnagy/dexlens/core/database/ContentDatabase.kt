package com.arthurnagy.dexlens.core.database

import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.arthurnagy.dexlens.core.database.entity.FlavorTextEntity

@Dao
interface ContentDao {
    @Upsert
    suspend fun upsertAll(rows: List<FlavorTextEntity>)

    @Query("SELECT * FROM flavor_text WHERE species_id = :speciesId AND language_id = :languageId ORDER BY version_id")
    suspend fun flavorText(
        speciesId: Int,
        languageId: Int,
    ): List<FlavorTextEntity>

    @Query("DELETE FROM flavor_text")
    suspend fun deleteAll()

    /** Swaps every stored row in one transaction, so readers never see a half-imported table. */
    @Transaction
    suspend fun replaceAll(rows: List<FlavorTextEntity>) {
        deleteAll()
        upsertAll(rows)
    }
}

@Database(entities = [FlavorTextEntity::class], version = 1, exportSchema = true)
abstract class ContentDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao
}
