package com.arthurnagy.dexlens.core.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "egg_group")
data class EggGroupEntity(
    @PrimaryKey val id: Int,
    val identifier: String,
    val name: String,
)
