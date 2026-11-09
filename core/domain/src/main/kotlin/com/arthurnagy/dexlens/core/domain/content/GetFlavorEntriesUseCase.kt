package com.arthurnagy.dexlens.core.domain.content

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.UseCase
import com.arthurnagy.dexlens.core.model.FlavorEntry
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.github.michaelbull.result.Result
import javax.inject.Inject

class GetFlavorEntriesUseCase
    @Inject
    constructor(
        private val repository: FlavorTextRepository,
    ) : UseCase {
        suspend operator fun invoke(id: SpeciesId): Result<List<FlavorEntry>, DomainError> = repository.flavorEntries(id)
    }
