package com.arthurnagy.dexlens.core.database

import androidx.room3.Dao
import androidx.room3.Query

@Dao
fun interface GameVersionDao {
    @Query("SELECT * FROM game_version")
    suspend fun gameVersions(): List<GameVersionEntity>
}
