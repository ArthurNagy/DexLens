package com.arthurnagy.dexlens.feature.pokedex.impl

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.species.SearchSpeciesUseCase
import com.arthurnagy.dexlens.core.model.PokemonType
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.model.SpeciesNameHit
import com.arthurnagy.dexlens.core.model.SpeciesSearchResult
import com.arthurnagy.dexlens.core.testing.FakeSpeciesRepository
import com.arthurnagy.dexlens.core.testing.speciesSummary
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import kotlin.test.Test

class PokedexViewModelTest {
    private val pikachu = speciesSummary(25, "Pikachu", PokemonType.ELECTRIC)
    private val bulbasaur = speciesSummary(1, "Bulbasaur", PokemonType.GRASS, PokemonType.POISON)
    private val repository = FakeSpeciesRepository(summaries = listOf(bulbasaur, pikachu))

    private fun results(vararg ids: Int) = listOf(bulbasaur, pikachu).filter {
        it.id.value in ids
    }.map { SpeciesSearchResult(it, matchedName = null) }

    private fun viewModel() = PokedexViewModel(SearchSpeciesUseCase(repository))

    // Scenario: List in Pokédex order
    @Test
    fun loadsEverySpeciesInOrder() = runTest {
        viewModel().testWithInternalState(this, PokedexState.Initial) {
            runOnCreate()
            expectInternalState { copy(loading = false, error = null, query = "", results = results(1, 25)) }
            cancelAndIgnoreRemainingItems()
        }
    }

    // Scenario: Prefix updates as you type
    @Test
    fun aQueryUpdatesResultsWithoutSubmitting() = runTest {
        repository.hits = mapOf("\"pika\"*" to listOf(SpeciesNameHit(SpeciesId(25), "Pikachu")))
        viewModel().testWithInternalState(this, PokedexState.Initial) {
            runOnCreate()
            expectInternalState { copy(loading = false, error = null, query = "", results = results(1, 25)) }
            containerHost.onQueryChanged("pika")
            expectInternalState { copy(query = "pika", results = results(25)) }
            cancelAndIgnoreRemainingItems()
        }
    }

    // Scenario: Shows error state
    @Test
    fun anErrorIsShownAndRetryRecovers() = runTest {
        repository.nextError = DomainError.Unknown("broken")
        viewModel().testWithInternalState(this, PokedexState.Initial) {
            runOnCreate()
            expectInternalState { copy(loading = false, error = DomainError.Unknown("broken"), query = "", results = emptyList()) }
            containerHost.onRetry()
            expectInternalState { copy(loading = true, error = null) }
            expectInternalState { copy(loading = false, error = null, query = "", results = results(1, 25)) }
            cancelAndIgnoreRemainingItems()
        }
    }
}
