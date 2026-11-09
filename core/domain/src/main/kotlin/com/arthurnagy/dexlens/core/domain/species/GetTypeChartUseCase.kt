package com.arthurnagy.dexlens.core.domain.species

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.UseCase
import com.arthurnagy.dexlens.core.model.TypeChart
import com.github.michaelbull.result.Result
import javax.inject.Inject

class GetTypeChartUseCase
    @Inject
    constructor(
        private val repository: TypeChartRepository,
    ) : UseCase {
        suspend operator fun invoke(): Result<TypeChart, DomainError> = repository.typeChart()
    }
