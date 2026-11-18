package com.arthurnagy.dexlens.core.domain.species

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.UseCase
import com.arthurnagy.dexlens.core.model.GenerationStart
import com.github.michaelbull.result.Result
import javax.inject.Inject

/** Where each generation starts in Pokédex order, so the list can jump between them. */
class GetGenerationStartsUseCase
    @Inject
    constructor(
        private val repository: SpeciesRepository,
    ) : UseCase {
        suspend operator fun invoke(): Result<List<GenerationStart>, DomainError> = repository.generationStarts()
    }
