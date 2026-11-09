package com.arthurnagy.dexlens.core.domain.species

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.UseCase
import com.arthurnagy.dexlens.core.model.SpeciesDetail
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.github.michaelbull.result.Result
import javax.inject.Inject

class GetSpeciesDetailUseCase
    @Inject
    constructor(
        private val repository: SpeciesRepository,
    ) : UseCase {
        suspend operator fun invoke(id: SpeciesId): Result<SpeciesDetail, DomainError> = repository.detail(id)
    }
