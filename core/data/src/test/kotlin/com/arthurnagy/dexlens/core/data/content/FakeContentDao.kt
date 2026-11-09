package com.arthurnagy.dexlens.core.data.content

import com.arthurnagy.dexlens.core.database.ContentDao
import com.arthurnagy.dexlens.core.database.FlavorTextEntity

internal class FakeContentDao : ContentDao {
    val rows = mutableListOf<FlavorTextEntity>()

    override suspend fun upsertAll(rows: List<FlavorTextEntity>) {
        this.rows += rows
    }

    override suspend fun flavorText(
        speciesId: Int,
        languageId: Int,
    ): List<FlavorTextEntity> = rows.filter { it.speciesId == speciesId && it.languageId == languageId }

    override suspend fun deleteAll() {
        rows.clear()
    }
}
