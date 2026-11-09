package com.arthurnagy.dexlens.core.database

import androidx.room3.Dao
import androidx.room3.Query

@Dao
fun interface TypeChartDao {
    @Query("SELECT * FROM type_efficacy")
    suspend fun typeChart(): List<TypeEfficacyEntity>
}
