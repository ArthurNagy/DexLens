package com.arthurnagy.dexlens.core.data.species

import com.arthurnagy.dexlens.core.data.catchStorage
import com.arthurnagy.dexlens.core.database.TypeChartDao
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.species.TypeChartRepository
import com.arthurnagy.dexlens.core.model.TypeChart
import com.github.michaelbull.result.Result
import javax.inject.Inject

internal class OfflineTypeChartRepository
    @Inject
    constructor(
        private val dao: TypeChartDao,
    ) : TypeChartRepository {
        override suspend fun typeChart(): Result<TypeChart, DomainError> = catchStorage { dao.typeChart().toTypeChart() }
    }
