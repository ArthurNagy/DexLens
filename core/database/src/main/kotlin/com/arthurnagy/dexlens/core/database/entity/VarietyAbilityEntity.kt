package com.arthurnagy.dexlens.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index

@Entity(tableName = "variety_ability", primaryKeys = ["variety_id", "slot"], indices = [Index("ability_id")])
internal data class VarietyAbilityEntity(
    @ColumnInfo(name = "variety_id") val varietyId: Int,
    val slot: Int,
    @ColumnInfo(name = "ability_id") val abilityId: Int,
    @ColumnInfo(name = "is_hidden") val isHidden: Boolean,
)
