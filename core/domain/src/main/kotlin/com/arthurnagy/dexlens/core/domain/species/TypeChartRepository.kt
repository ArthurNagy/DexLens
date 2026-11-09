package com.arthurnagy.dexlens.core.domain.species

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.model.TypeChart
import com.github.michaelbull.result.Result

interface TypeChartRepository {
    suspend fun typeChart(): Result<TypeChart, DomainError>
}
