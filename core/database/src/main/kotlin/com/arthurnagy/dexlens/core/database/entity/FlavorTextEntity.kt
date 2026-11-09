package com.arthurnagy.dexlens.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity

/** Flavor text the user chose to download. It is never bundled, so it lives apart from the prebuilt database. */
@Entity(tableName = "flavor_text", primaryKeys = ["species_id", "version_id", "language_id"])
data class FlavorTextEntity(
    @ColumnInfo(name = "species_id") val speciesId: Int,
    @ColumnInfo(name = "version_id") val versionId: Int,
    @ColumnInfo(name = "language_id") val languageId: Int,
    val text: String,
)
