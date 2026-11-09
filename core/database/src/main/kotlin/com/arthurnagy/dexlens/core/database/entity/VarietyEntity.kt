package com.arthurnagy.dexlens.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** A `pokemon` row: a battle-distinct variety of a species, with its own types, stats and abilities. */
@Entity(tableName = "variety", indices = [Index("species_id")])
data class VarietyEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "species_id") val speciesId: Int,
    val identifier: String,
    val name: String,
    @ColumnInfo(name = "is_default") val isDefault: Boolean,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    /** Decimetres. */
    val height: Int,
    /** Hectograms. */
    val weight: Int,
    @ColumnInfo(name = "type_primary") val typePrimary: Int,
    @ColumnInfo(name = "type_secondary") val typeSecondary: Int?,
    val hp: Int,
    val attack: Int,
    val defense: Int,
    @ColumnInfo(name = "special_attack") val specialAttack: Int,
    @ColumnInfo(name = "special_defense") val specialDefense: Int,
    val speed: Int,
)
