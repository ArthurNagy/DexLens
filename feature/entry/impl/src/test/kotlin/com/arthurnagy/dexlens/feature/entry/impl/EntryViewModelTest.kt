package com.arthurnagy.dexlens.feature.entry.impl

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.content.ObserveArtworkUseCase
import com.arthurnagy.dexlens.core.domain.content.ObserveFlavorEntriesUseCase
import com.arthurnagy.dexlens.core.domain.species.GetSpeciesDetailUseCase
import com.arthurnagy.dexlens.core.domain.species.GetTypeChartUseCase
import com.arthurnagy.dexlens.core.model.ArtworkLocation
import com.arthurnagy.dexlens.core.model.FlavorEntry
import com.arthurnagy.dexlens.core.model.PokemonType
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.testing.FakeContentPackRepository
import com.arthurnagy.dexlens.core.testing.FakeFlavorTextRepository
import com.arthurnagy.dexlens.core.testing.FakeSpeciesRepository
import com.arthurnagy.dexlens.core.testing.FakeTypeChartRepository
import com.arthurnagy.dexlens.core.testing.bulbasaurDetail
import com.arthurnagy.dexlens.core.testing.sampleTypeChart
import com.arthurnagy.dexlens.feature.entry.api.EntryKey
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import kotlin.test.Test

class EntryViewModelTest {
    private val chart = sampleTypeChart()
    private val base = bulbasaurDetail()
    private val mega = base.varieties.single().copy(
        id = 2,
        name = "Mega Bulbasaur",
        isDefault = false,
        types = listOf(PokemonType.GRASS, PokemonType.FIRE),
    )
    private val detail = base.copy(varieties = base.varieties + mega)
    private val repository = FakeSpeciesRepository(details = mapOf(SpeciesId(1) to detail))
    private val charts = FakeTypeChartRepository(chart)

    private val content = FakeContentPackRepository()
    private val flavor = FakeFlavorTextRepository()

    private fun viewModel(speciesId: Int = 1) = EntryViewModel(
        EntryKey(speciesId),
        GetSpeciesDetailUseCase(repository),
        GetTypeChartUseCase(charts),
        ObserveArtworkUseCase(content),
        ObserveFlavorEntriesUseCase(flavor),
    )

    private val loaded = EntryState.loaded(detail, chart, ArtworkLocation(null), emptyList())

    // Scenario: Entry shows full data
    @Test
    fun loadsTheSpeciesAndItsMatchups() = runTest {
        viewModel().testWithInternalState(this, EntryState.Initial) {
            runOnCreate()
            expectInternalState { loaded }
            cancelAndIgnoreRemainingItems()
        }
    }

    // Scenario: Unknown species
    @Test
    fun anUnknownSpeciesEndsInNotFound() = runTest {
        viewModel(speciesId = 999).testWithInternalState(this, EntryState.Initial) {
            runOnCreate()
            expectInternalState { EntryState.Failed(DomainError.NotFound) }
        }
    }

    @Test
    fun aChartFailureIsAnError() = runTest {
        charts.nextError = DomainError.Unknown("broken")
        viewModel().testWithInternalState(this, EntryState.Initial) {
            runOnCreate()
            expectInternalState { EntryState.Failed(DomainError.Unknown("broken")) }
        }
    }

    // Scenario: Selecting a variety updates the entry
    @Test
    fun selectingAVarietyRecalculatesMatchups() = runTest {
        viewModel().testWithInternalState(this, EntryState.Initial) {
            runOnCreate()
            expectInternalState { loaded }
            containerHost.onSelectVariety(2)
            expectInternalState { loaded.select(mega) }
            cancelAndIgnoreRemainingItems()
        }
    }

    // Scenario: Art appears when the download finishes
    @Test
    fun anOpenEntryPicksUpArtworkAndKeepsTheSelectedVariety() = runTest {
        val location = ArtworkLocation("/art")
        viewModel().testWithInternalState(this, EntryState.Initial) {
            runOnCreate()
            expectInternalState { loaded }
            containerHost.onSelectVariety(2)
            expectInternalState { loaded.select(mega) }
            content.artwork.value = location
            expectInternalState { loaded.select(mega).copy(artwork = location) }
            cancelAndIgnoreRemainingItems()
        }
    }

    // Scenario: Flavor text appears in an open entry
    @Test
    fun anOpenEntryPicksUpFlavorText() = runTest {
        val entries = listOf(FlavorEntry("A seed grows.", listOf("Red", "Blue")))
        viewModel().testWithInternalState(this, EntryState.Initial) {
            runOnCreate()
            expectInternalState { loaded }
            flavor.entries.value = entries
            expectInternalState { loaded.copy(flavor = entries) }
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun selectingAnUnknownVarietyDoesNothing() = runTest {
        viewModel().testWithInternalState(this, loaded) {
            containerHost.onSelectVariety(42)
        }
    }

    // Scenario: Flavor text is imported
    @Test
    fun loadsDownloadedArtAndFlavor() = runTest {
        val entries = listOf(FlavorEntry("A seed grows.", listOf("Red", "Blue")))
        val location = ArtworkLocation("/art")
        val downloaded = FakeContentPackRepository(artwork = location)
        val downloadedText = FakeFlavorTextRepository(entries)
        val viewModel = EntryViewModel(
            EntryKey(1),
            GetSpeciesDetailUseCase(repository),
            GetTypeChartUseCase(charts),
            ObserveArtworkUseCase(downloaded),
            ObserveFlavorEntriesUseCase(downloadedText),
        )
        viewModel.testWithInternalState(this, EntryState.Initial) {
            runOnCreate()
            expectInternalState { EntryState.loaded(detail, chart, location, entries) }
            cancelAndIgnoreRemainingItems()
        }
    }
}
