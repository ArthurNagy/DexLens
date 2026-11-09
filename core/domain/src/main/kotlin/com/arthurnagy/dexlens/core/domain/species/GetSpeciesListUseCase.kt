package com.arthurnagy.dexlens.core.domain.species

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.UseCase
import com.arthurnagy.dexlens.core.model.SpeciesSummary
import com.github.michaelbull.result.Result
import javax.inject.Inject

class GetSpeciesListUseCase
    @Inject
    constructor(
        private val repository: SpeciesRepository,
    ) : UseCase {
        suspend operator fun invoke(): Result<List<SpeciesSummary>, DomainError> = repository.summaries()
    }
