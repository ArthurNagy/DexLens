package com.arthurnagy.dexlens.core.domain.species

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.model.PokemonType
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.model.SpeciesNameHit
import com.arthurnagy.dexlens.core.testing.FakeSpeciesRepository
import com.arthurnagy.dexlens.core.testing.speciesSummary
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.getOrThrow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

internal class SearchSpeciesUseCaseTest {
    private val bulbasaur = speciesSummary(1, "Bulbasaur", PokemonType.GRASS, PokemonType.POISON)
    private val pikachu = speciesSummary(25, "Pikachu", PokemonType.ELECTRIC)
    private val mrMime = speciesSummary(122, "Mr. Mime", PokemonType.PSYCHIC, PokemonType.FAIRY)
    private val mewtwo = speciesSummary(150, "Mewtwo", PokemonType.PSYCHIC)
    private val mew = speciesSummary(151, "Mew", PokemonType.PSYCHIC)
    private val unown250 = speciesSummary(250, "Ho-Oh", PokemonType.FIRE, PokemonType.FLYING)
    private val flabebe = speciesSummary(669, "Flabébé", PokemonType.FAIRY)
    private val repository = FakeSpeciesRepository(summaries = listOf(bulbasaur, pikachu, mrMime, mewtwo, mew, unown250, flabebe))
    private val search = SearchSpeciesUseCase(repository)

    private fun hit(
        id: Int,
        name: String,
    ) = SpeciesNameHit(SpeciesId(id), name)

    // Scenario: Clearing restores the list
    @Test
    fun blankQueryReturnsAllInOrder() = runTest {
        assertEquals(repository.summaries, search("  ").getOrThrow { error("unexpected") }.map { it.summary })
    }

    // Scenario: Prefix updates as you type
    @Test
    fun prefixMatches() = runTest {
        repository.hits = mapOf("\"pika\"*" to listOf(hit(25, "Pikachu")))
        val results = search("pika").getOrThrow { error("unexpected") }
        assertEquals(listOf(pikachu), results.map { it.summary })
        assertNull(results.single().matchedName)
    }

    // Scenario: Accents ignored
    @Test
    fun accentsIgnored() = runTest {
        repository.hits = mapOf("\"flabebe\"*" to listOf(hit(669, "Flabébé")))
        assertEquals(listOf(flabebe), search("flabebe").getOrThrow { error("unexpected") }.map { it.summary })
    }

    // Scenario: Punctuation ignored in search
    @Test
    fun punctuationIgnored() = runTest {
        repository.hits = mapOf("\"mr\"* \"mime\"*" to listOf(hit(122, "Mr. Mime")))
        assertEquals(listOf(mrMime), search("mr mime").getOrThrow { error("unexpected") }.map { it.summary })
    }

    // Scenario: Best match listed first
    @Test
    fun bestMatchFirst() = runTest {
        repository.hits = mapOf("\"mew\"*" to listOf(hit(150, "Mewtwo"), hit(151, "Mew")))
        assertEquals(listOf(mew, mewtwo), search("mew").getOrThrow { error("unexpected") }.map { it.summary })
    }

    // Scenario: No results shows empty state
    @Test
    fun noResultsIsEmpty() = runTest {
        assertTrue(search("zzzz").getOrThrow { error("unexpected") }.isEmpty())
    }

    // Scenario: Search by number
    @Test
    fun numberFindsExactFirst() = runTest {
        val results = search("25").getOrThrow { error("unexpected") }
        assertEquals(listOf(pikachu, unown250), results.map { it.summary })
    }

    // Scenario: Search by padded number
    @Test
    fun paddedNumberIsExact() = runTest {
        assertEquals(listOf(pikachu), search("#025").getOrThrow { error("unexpected") }.map { it.summary })
        assertEquals(listOf(pikachu), search("025").getOrThrow { error("unexpected") }.map { it.summary })
    }

    // Scenario: Search other language
    @Test
    fun otherLanguageShowsMatchedName() = runTest {
        repository.hits = mapOf("\"bulbizarre\"*" to listOf(hit(1, "Bulbizarre")))
        val result = search("Bulbizarre").getOrThrow { error("unexpected") }.single()
        assertEquals(bulbasaur, result.summary)
        assertEquals("Bulbizarre", result.matchedName)
    }

    // Scenario: Search Japanese name
    @Test
    fun japaneseSearch() = runTest {
        repository.hits = mapOf("\"ピカチュウ\"*" to listOf(hit(25, "ピカチュウ")))
        assertEquals(listOf(pikachu), search("ピカチュウ").getOrThrow { error("unexpected") }.map { it.summary })
    }

    @Test
    fun unsearchableQueryIsEmpty() = runTest {
        assertTrue(search("?!").getOrThrow { error("unexpected") }.isEmpty())
    }

    @Test
    fun repositoryErrorIsReturned() = runTest {
        repository.nextError = DomainError.Unknown("boom")
        assertEquals(Err(DomainError.Unknown("boom")), search("pika"))
    }

    @Test
    fun displayedNameWinsOverOtherLanguageMatch() = runTest {
        repository.hits = mapOf("\"pikachu\"*" to listOf(hit(25, "Pikachu"), hit(25, "Pikachú")))
        assertNull(search("pikachu").getOrThrow { error("unexpected") }.single().matchedName)
    }
}
