package com.arthurnagy.dexlens.core.database.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** Game versions, used to label downloaded flavor text. */
@Entity(tableName = "game_version")
data class GameVersionEntity(
    @PrimaryKey val id: Int,
    val name: String,
)
