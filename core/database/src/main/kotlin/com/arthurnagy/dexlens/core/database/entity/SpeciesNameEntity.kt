package com.arthurnagy.dexlens.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index

@Entity(tableName = "species_name", primaryKeys = ["species_id", "language_id"], indices = [Index("language_id")])
data class SpeciesNameEntity(
    @ColumnInfo(name = "species_id") val speciesId: Int,
    @ColumnInfo(name = "language_id") val languageId: Int,
    val name: String,
    val genus: String?,
)
