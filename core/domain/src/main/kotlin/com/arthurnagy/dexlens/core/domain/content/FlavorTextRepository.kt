package com.arthurnagy.dexlens.core.domain.content

import com.arthurnagy.dexlens.core.model.FlavorEntry
import com.arthurnagy.dexlens.core.model.SpeciesId
import kotlinx.coroutines.flow.Flow

/** Downloaded in-game Pokédex text. */
interface FlavorTextRepository {
    /** A species' entries as they change: empty until the optional download has run. Never throws. */
    fun observeFlavorEntries(id: SpeciesId): Flow<List<FlavorEntry>>
}
