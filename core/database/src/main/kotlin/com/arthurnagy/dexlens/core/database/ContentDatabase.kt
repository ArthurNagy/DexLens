package com.arthurnagy.dexlens.core.database

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.Transaction
import androidx.room3.Upsert

/** Flavor text the user chose to download. It is never bundled, so it lives apart from the prebuilt database. */
@Entity(tableName = "flavor_text", primaryKeys = ["species_id", "version_id", "language_id"])
data class FlavorTextEntity(
    @ColumnInfo(name = "species_id") val speciesId: Int,
    @ColumnInfo(name = "version_id") val versionId: Int,
    @ColumnInfo(name = "language_id") val languageId: Int,
    val text: String,
)

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
