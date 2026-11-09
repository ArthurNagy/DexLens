package com.arthurnagy.dexlens.core.data.content

import com.arthurnagy.dexlens.core.database.ContentDao
import com.arthurnagy.dexlens.core.database.entity.FlavorTextEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

internal class FakeContentDao : ContentDao {
    private val table = MutableStateFlow<List<FlavorTextEntity>>(emptyList())

    val rows: List<FlavorTextEntity> get() = table.value

    override suspend fun upsertAll(rows: List<FlavorTextEntity>) {
        table.update { it + rows }
    }

    override fun observeFlavorText(
        speciesId: Int,
        languageIds: Set<Int>,
    ): Flow<List<FlavorTextEntity>> = table.map { all -> all.filter { it.speciesId == speciesId && it.languageId in languageIds } }

    override suspend fun deleteAll() {
        table.value = emptyList()
    }
}
