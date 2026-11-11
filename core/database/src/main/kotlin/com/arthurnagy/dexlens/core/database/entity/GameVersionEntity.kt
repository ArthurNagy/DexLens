package com.arthurnagy.dexlens.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** Game versions, used to label downloaded flavor text and to group encounters into games. */
@Entity(tableName = "game_version")
data class GameVersionEntity(
    @PrimaryKey val id: Int,
    val name: String,
    /** The DLC versions keep their own group, which has no [VersionGroupEntity]: their encounters are filed under the base game. */
    @ColumnInfo(name = "version_group_id") val versionGroupId: Int,
)
