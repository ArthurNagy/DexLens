package com.arthurnagy.dexlens.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index

@Entity(tableName = "species_egg_group", primaryKeys = ["species_id", "egg_group_id"], indices = [Index("egg_group_id")])
internal data class SpeciesEggGroupEntity(
    @ColumnInfo(name = "species_id") val speciesId: Int,
    @ColumnInfo(name = "egg_group_id") val eggGroupId: Int,
)
