package com.arthurnagy.dexlens.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** PokeAPI's own wording ("ability_prose"), never in-game text. */
@Entity(tableName = "ability")
data class AbilityEntity(
    @PrimaryKey val id: Int,
    val identifier: String,
    val name: String,
    @ColumnInfo(name = "short_effect") val shortEffect: String?,
    val effect: String?,
)
