package com.arthurnagy.dexlens.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** A `pokemon_forms` row: a visual or battle-only form of a variety. */
@Entity(tableName = "form", indices = [Index("variety_id")])
data class FormEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "variety_id") val varietyId: Int,
    val identifier: String,
    val name: String,
    @ColumnInfo(name = "is_default") val isDefault: Boolean,
    @ColumnInfo(name = "is_battle_only") val isBattleOnly: Boolean,
    @ColumnInfo(name = "is_mega") val isMega: Boolean,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "type_primary") val typePrimary: Int?,
    @ColumnInfo(name = "type_secondary") val typeSecondary: Int?,
)
