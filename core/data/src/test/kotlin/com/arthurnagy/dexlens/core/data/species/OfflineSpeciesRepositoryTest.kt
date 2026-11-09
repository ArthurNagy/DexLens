package com.arthurnagy.dexlens.core.data.species

import com.arthurnagy.dexlens.core.database.ChainSpeciesRow
import com.arthurnagy.dexlens.core.database.ScanNameRow
import com.arthurnagy.dexlens.core.database.SpeciesSearchRow
import com.arthurnagy.dexlens.core.database.SpeciesSummaryRow
import com.arthurnagy.dexlens.core.database.VarietyAbilityRow
import com.arthurnagy.dexlens.core.database.entity.EggGroupEntity
import com.arthurnagy.dexlens.core.database.entity.EvolutionEntity
import com.arthurnagy.dexlens.core.database.entity.LanguageEntity
import com.arthurnagy.dexlens.core.database.entity.SpeciesEntity
import com.arthurnagy.dexlens.core.database.entity.SpeciesNameEntity
import com.arthurnagy.dexlens.core.database.entity.VarietyEntity
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.model.EvolutionTrigger
import com.arthurnagy.dexlens.core.model.PokemonType
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.getOrThrow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

internal class OfflineSpeciesRepositoryTest {
    private val dao = FakeSpeciesDao()
    private var language = ENGLISH
    private val repository = OfflineSpeciesRepository(dao) { language }

    private fun evolution(
        id: Int,
        species: Int,
        from: Int,
        trigger: String,
        level: Int? = null,
        item: String? = null,
    ) = EvolutionEntity(
        id,
        species,
        from,
        trigger,
        level,
        item,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        needsRain = false,
        turnUpsideDown = false,
        usedMove = null,
        minMoveCount = null,
        minSteps = null,
        minDamageTaken = null,
    )

    private fun chainRow(
        id: Int,
        name: String,
        from: Int?,
    ) = ChainSpeciesRow(id, name, from, GRASS, POISON)

    private fun seedBulbasaur() {
        dao.summaries = listOf(
            SpeciesSummaryRow(1, "Bulbasaur", GRASS, POISON),
            SpeciesSummaryRow(2, "Ivysaur", GRASS, POISON),
            SpeciesSummaryRow(3, "Venusaur", GRASS, POISON),
        )
        dao.species =
            listOf(
                SpeciesEntity(
                    1,
                    "bulbasaur",
                    1,
                    genderRate = 1,
                    evolvesFromSpeciesId = null,
                    evolutionChainId = 1,
                    isBaby = false,
                    isLegendary = false,
                    isMythical = false,
                ),
            )
        dao.names = listOf(
            SpeciesNameEntity(1, ENGLISH, "Bulbasaur", "Seed Pokémon"),
            SpeciesNameEntity(1, FRENCH, "Bulbizarre", "Pokémon Graine"),
            SpeciesNameEntity(1, ROMAJI, "Fushigidane", null),
        )
        dao.languages =
            listOf(
                LanguageEntity(ENGLISH, "en", "English"),
                LanguageEntity(FRENCH, "fr", "French"),
                LanguageEntity(ROMAJI, "roomaji", "Romaji"),
            )
        dao.varieties = listOf(VarietyEntity(1, 1, "bulbasaur", "Bulbasaur", true, 1, 7, 69, GRASS, POISON, 45, 49, 49, 65, 65, 45))
        dao.abilities = listOf(
            VarietyAbilityRow(1, 1, false, "Overgrow", "Short", "Long"),
            VarietyAbilityRow(1, 3, true, "Chlorophyll", "Short", "Long"),
        )
        dao.eggGroups = listOf(EggGroupEntity(1, "monster", "Monster"), EggGroupEntity(7, "plant", "Grass"))
        dao.chain = listOf(chainRow(1, "Bulbasaur", null), chainRow(2, "Ivysaur", 1), chainRow(3, "Venusaur", 2))
        dao.evolutions = listOf(evolution(1, 2, 1, "level-up", level = 16), evolution(2, 3, 2, "level-up", level = 32))
    }

    // Scenario: Entry shows full data
    @Test
    fun detailMapsEveryField() = runTest {
        seedBulbasaur()
        val detail = repository.detail(SpeciesId(1)).getOrThrow { error("unexpected") }
        assertEquals("Bulbasaur", detail.summary.name)
        assertEquals(listOf(PokemonType.GRASS, PokemonType.POISON), detail.summary.types)
        assertEquals("Seed Pokémon", detail.genus)
        assertEquals(1, detail.genderRatio.femaleEighths)
        assertEquals(listOf("Monster", "Grass"), detail.eggGroups)
        val variety = detail.varieties.single()
        assertEquals(7, variety.heightDecimetres)
        assertEquals(69, variety.weightHectograms)
        assertEquals(318, variety.stats.total)
        assertEquals(listOf("Overgrow", "Chlorophyll"), variety.abilities.map { it.name })
        assertEquals(listOf(false, true), variety.abilities.map { it.isHidden })
        assertEquals(listOf("French" to "Bulbizarre"), detail.otherNames.map { it.language to it.name })
    }

    // Scenario: Genderless species
    @Test
    fun genderlessSpeciesHasNoRatio() = runTest {
        seedBulbasaur()
        dao.species = dao.species.map { it.copy(genderRate = -1) }
        assertNull(repository.detail(SpeciesId(1)).getOrThrow { error("unexpected") }.genderRatio.femaleEighths)
    }

    // Scenario: Unknown species
    @Test
    fun unknownSpeciesIsNotFound() = runTest {
        seedBulbasaur()
        assertEquals(Err(DomainError.NotFound), repository.detail(SpeciesId(9999)))
    }

    // Scenario: Evolution line with triggers
    @Test
    fun evolutionLineHasTriggers() = runTest {
        seedBulbasaur()
        val root = repository.detail(SpeciesId(1)).getOrThrow { error("unexpected") }.evolutionLine
        assertEquals("Bulbasaur", root.species.name)
        assertEquals(emptyList(), root.methods)
        val ivysaur = root.children.single()
        assertEquals(16, ivysaur.methods.single().minLevel)
        assertEquals(EvolutionTrigger.LEVEL_UP, ivysaur.methods.single().trigger)
        assertEquals(32, ivysaur.children.single().methods.single().minLevel)
    }

    // Scenario: Branching evolution
    @Test
    fun branchingEvolutionKeepsEachTrigger() = runTest {
        seedBulbasaur()
        dao.chain = listOf(chainRow(133, "Eevee", null), chainRow(134, "Vaporeon", 133), chainRow(135, "Jolteon", 133))
        dao.evolutions =
            listOf(evolution(1, 134, 133, "use-item", item = "Water Stone"), evolution(2, 135, 133, "use-item", item = "Thunder Stone"))
        val root = repository.detail(SpeciesId(1)).getOrThrow { error("unexpected") }.evolutionLine
        assertEquals(listOf("Water Stone", "Thunder Stone"), root.children.map { it.methods.single().item })
    }

    @Test
    fun summariesAreCachedPerLanguage() = runTest {
        seedBulbasaur()
        repository.summaries()
        repository.summaries()
        assertEquals(1, dao.summaryLoads)
        language = FRENCH
        repository.summaries()
        assertEquals(2, dao.summaryLoads)
    }

    @Test
    fun searchAndScanMapNames() = runTest {
        dao.searchRows = listOf(SpeciesSearchRow(1, FRENCH, "Bulbizarre"))
        dao.scanRows = listOf(ScanNameRow(1, "Bulbasaur"))
        assertEquals("Bulbizarre", repository.searchNames("x").getOrThrow { error("unexpected") }.single().name)
        assertEquals(SpeciesId(1), repository.scanNames().getOrThrow { error("unexpected") }.single().speciesId)
    }

    @Test
    fun failuresBecomeDomainErrors() = runTest {
        dao.failure = IllegalStateException("broken")
        assertEquals(Err(DomainError.Unknown("broken")), repository.summaries())
        assertEquals(Err(DomainError.Unknown("broken")), repository.searchNames("x"))
        assertEquals(Err(DomainError.Unknown("broken")), repository.scanNames())
        assertEquals(Err(DomainError.Unknown("broken")), repository.detail(SpeciesId(1)))
    }

    @Test
    fun cancellationPropagates() = runTest {
        dao.failure = CancellationException("stop")
        assertFailsWith<CancellationException> { repository.summaries() }
    }

    private companion object {
        const val ROMAJI = 2
        const val FRENCH = 5
        const val GRASS = 12
        const val POISON = 4
    }
}
