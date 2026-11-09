package com.arthurnagy.dexlens.core.data.species

import com.arthurnagy.dexlens.core.database.TypeChartDao
import com.arthurnagy.dexlens.core.database.TypeEfficacyEntity
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.model.PokemonType
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.getOrThrow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

internal class OfflineTypeChartRepositoryTest {
    @Test
    fun mapsChart() = runTest {
        val dao = TypeChartDao { listOf(TypeEfficacyEntity(10, 12, 200)) }
        val chart = OfflineTypeChartRepository(dao).typeChart().getOrThrow { error("unexpected") }
        assertEquals(200, chart.factors[PokemonType.FIRE to PokemonType.GRASS])
    }

    @Test
    fun failureBecomesDomainError() = runTest {
        val dao = TypeChartDao { throw IllegalStateException("broken") }
        assertEquals(Err(DomainError.Unknown("broken")), OfflineTypeChartRepository(dao).typeChart())
    }
}
