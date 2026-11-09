package com.arthurnagy.dexlens.core.domain.content

import com.arthurnagy.dexlens.core.domain.UseCase
import com.arthurnagy.dexlens.core.model.FlavorEntry
import com.arthurnagy.dexlens.core.model.SpeciesId
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveFlavorEntriesUseCase
    @Inject
    constructor(
        private val repository: FlavorTextRepository,
    ) : UseCase {
        operator fun invoke(id: SpeciesId): Flow<List<FlavorEntry>> = repository.observeFlavorEntries(id)
    }
