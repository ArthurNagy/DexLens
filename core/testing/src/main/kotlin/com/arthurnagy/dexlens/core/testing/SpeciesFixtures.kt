package com.arthurnagy.dexlens.core.testing

import com.arthurnagy.dexlens.core.model.Ability
import com.arthurnagy.dexlens.core.model.BaseStats
import com.arthurnagy.dexlens.core.model.EvolutionMethod
import com.arthurnagy.dexlens.core.model.EvolutionNode
import com.arthurnagy.dexlens.core.model.EvolutionTrigger
import com.arthurnagy.dexlens.core.model.GenderRatio
import com.arthurnagy.dexlens.core.model.GenerationStart
import com.arthurnagy.dexlens.core.model.PokemonType
import com.arthurnagy.dexlens.core.model.SpeciesDetail
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.model.SpeciesSummary
import com.arthurnagy.dexlens.core.model.Variety

fun speciesSummary(
    id: Int,
    name: String,
    vararg types: PokemonType,
): SpeciesSummary = SpeciesSummary(id = SpeciesId(id), name = name, types = types.toList().ifEmpty { listOf(PokemonType.NORMAL) })

/** The nine generations' first species, as the bundled data has them. */
fun nationalGenerationStarts(): List<GenerationStart> =
    listOf(1, 152, 252, 387, 494, 650, 722, 810, 906).mapIndexed { index, first -> GenerationStart(index + 1, SpeciesId(first)) }

/** All 1,025 species in Pokédex order, named "Species <id>" except the generation starts the tests look for. */
fun nationalDexSummaries(): List<SpeciesSummary> = (1..NATIONAL_DEX_SIZE).map { id ->
    speciesSummary(id, NATIONAL_DEX_NAMES[id] ?: "Species $id", PokemonType.NORMAL)
}

private const val NATIONAL_DEX_SIZE = 1025

private val NATIONAL_DEX_NAMES = mapOf(1 to "Bulbasaur", 252 to "Treecko", 387 to "Turtwig", 906 to "Sprigatito")

fun levelUpMethod(level: Int): EvolutionMethod = EvolutionMethod(
    trigger = EvolutionTrigger.LEVEL_UP,
    minLevel = level,
    item = null,
    heldItem = null,
    timeOfDay = null,
    knownMove = null,
    knownMoveType = null,
    minHappiness = null,
    minBeauty = null,
    minAffection = null,
    relativePhysicalStats = null,
    partySpecies = null,
    partyType = null,
    tradeSpecies = null,
    location = null,
    region = null,
    gender = null,
    needsRain = false,
    turnUpsideDown = false,
    usedMove = null,
    minMoveCount = null,
    minSteps = null,
    minDamageTaken = null,
)

/** A complete Bulbasaur, the fixture screen tests and goldens use. */
fun bulbasaurDetail(): SpeciesDetail {
    val bulbasaur = speciesSummary(1, "Bulbasaur", PokemonType.GRASS, PokemonType.POISON)
    val ivysaur = speciesSummary(2, "Ivysaur", PokemonType.GRASS, PokemonType.POISON)
    val venusaur = speciesSummary(3, "Venusaur", PokemonType.GRASS, PokemonType.POISON)
    return SpeciesDetail(
        summary = bulbasaur,
        genus = "Seed Pokémon",
        generation = 1,
        genderRatio = GenderRatio(femaleEighths = 1),
        eggGroups = listOf("Grass", "Monster"),
        varieties = listOf(
            Variety(
                id = 1,
                name = "Bulbasaur",
                isDefault = true,
                heightDecimetres = 7,
                weightHectograms = 69,
                types = bulbasaur.types,
                stats = BaseStats(hp = 45, attack = 49, defense = 49, specialAttack = 65, specialDefense = 65, speed = 45),
                abilities = listOf(
                    Ability(
                        "Overgrow",
                        "Strengthens Grass moves at low HP.",
                        "Strengthens Grass moves to inflict 1.5× damage at 1/3 max HP or less.",
                        isHidden = false,
                    ),
                    Ability("Chlorophyll", "Doubles Speed during strong sunlight.", "Doubles Speed.", isHidden = true),
                ),
                forms = emptyList(),
            ),
        ),
        evolutionLine = EvolutionNode(
            species = bulbasaur,
            methods = emptyList(),
            children = listOf(
                EvolutionNode(
                    species = ivysaur,
                    methods = listOf(levelUpMethod(16)),
                    children = listOf(EvolutionNode(venusaur, listOf(levelUpMethod(32)), emptyList())),
                ),
            ),
        ),
    )
}
