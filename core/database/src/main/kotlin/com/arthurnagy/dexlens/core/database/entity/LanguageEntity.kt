package com.arthurnagy.dexlens.core.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "language")
data class LanguageEntity(
    @PrimaryKey val id: Int,
    val identifier: String,
    val name: String,
)
