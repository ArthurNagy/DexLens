package com.arthurnagy.dexlens.core.domain.scan

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.model.SpeciesNameHit
import com.arthurnagy.dexlens.core.testing.FakeSpeciesRepository
import com.arthurnagy.dexlens.core.testing.TestDispatcherProvider
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.getOrThrow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

internal class BuildSpeciesSignalUseCaseTest {
    @Test
    fun buildsSignalFromNames() = runTest {
        val repository = FakeSpeciesRepository(scanNames = listOf(SpeciesNameHit(SpeciesId(25), "Pikachu")))
        val signal = BuildSpeciesSignalUseCase(repository, TestDispatcherProvider(testScheduler))().getOrThrow { error("unexpected") }
        assertEquals(SpeciesId(25), signal.analyze(FrameObservation(listOf("PIKACHU"))).candidates.single().speciesId)
    }

    @Test
    fun errorIsReturned() = runTest {
        val repository = FakeSpeciesRepository().apply { nextError = DomainError.Unknown("boom") }
        assertEquals(Err(DomainError.Unknown("boom")), BuildSpeciesSignalUseCase(repository, TestDispatcherProvider(testScheduler))())
    }
}
