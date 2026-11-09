package com.arthurnagy.dexlens.core.testing

import com.arthurnagy.dexlens.core.domain.content.FlavorTextRepository
import com.arthurnagy.dexlens.core.model.FlavorEntry
import com.arthurnagy.dexlens.core.model.SpeciesId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeFlavorTextRepository(
    entries: List<FlavorEntry> = emptyList(),
) : FlavorTextRepository {
    val entries = MutableStateFlow(entries)

    override fun observeFlavorEntries(id: SpeciesId): Flow<List<FlavorEntry>> = entries
}
