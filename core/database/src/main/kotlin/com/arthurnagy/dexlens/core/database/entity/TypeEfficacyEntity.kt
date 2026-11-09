package com.arthurnagy.dexlens.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity

@Entity(tableName = "type_efficacy", primaryKeys = ["attacking_type", "defending_type"])
data class TypeEfficacyEntity(
    @ColumnInfo(name = "attacking_type") val attackingType: Int,
    @ColumnInfo(name = "defending_type") val defendingType: Int,
    /** Percent: 0, 50, 100 or 200. */
    val factor: Int,
)
