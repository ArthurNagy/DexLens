package com.arthurnagy.dexlens.core.domain.species

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.testing.FakeSpeciesRepository
import com.arthurnagy.dexlens.core.testing.FakeTypeChartRepository
import com.arthurnagy.dexlens.core.testing.bulbasaurDetail
import com.arthurnagy.dexlens.core.testing.nationalGenerationStarts
import com.arthurnagy.dexlens.core.testing.sampleTypeChart
import com.arthurnagy.dexlens.core.testing.speciesSummary
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

internal class SpeciesUseCasesTest {
    @Test
    fun listReturnsRepositorySummaries() = runTest {
        val summaries = listOf(speciesSummary(1, "Bulbasaur"))
        assertEquals(Ok(summaries), GetSpeciesListUseCase(FakeSpeciesRepository(summaries = summaries))())
    }

    @Test
    fun detailFound() = runTest {
        val detail = bulbasaurDetail()
        val repository = FakeSpeciesRepository(details = mapOf(SpeciesId(1) to detail))
        assertEquals(Ok(detail), GetSpeciesDetailUseCase(repository)(SpeciesId(1)))
    }

    // Scenario: Unknown species
    @Test
    fun detailMissingIsNotFound() = runTest {
        assertEquals(Err(DomainError.NotFound), GetSpeciesDetailUseCase(FakeSpeciesRepository())(SpeciesId(9999)))
    }

    @Test
    fun typeChartReturned() = runTest {
        val chart = sampleTypeChart()
        assertEquals(Ok(chart), GetTypeChartUseCase(FakeTypeChartRepository(chart))())
    }

    // Scenario: Dragging jumps to each generation's first species
    @Test
    fun generationStartsReturnsRepositoryStarts() = runTest {
        val starts = nationalGenerationStarts()
        assertEquals(Ok(starts), GetGenerationStartsUseCase(FakeSpeciesRepository(generationStarts = starts))())
    }

    @Test
    fun generationStartsPassesErrorsThrough() = runTest {
        val repository = FakeSpeciesRepository(generationStarts = nationalGenerationStarts()).apply { nextError = DomainError.NotFound }
        assertEquals(Err(DomainError.NotFound), GetGenerationStartsUseCase(repository)())
    }
}
