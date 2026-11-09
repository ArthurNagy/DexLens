package com.arthurnagy.dexlens.core.data.species

import com.arthurnagy.dexlens.core.database.ChainSpeciesRow
import com.arthurnagy.dexlens.core.database.SpeciesSummaryRow
import com.arthurnagy.dexlens.core.database.VarietyAbilityRow
import com.arthurnagy.dexlens.core.database.entity.EvolutionEntity
import com.arthurnagy.dexlens.core.database.entity.FormEntity
import com.arthurnagy.dexlens.core.database.entity.TypeEfficacyEntity
import com.arthurnagy.dexlens.core.database.entity.VarietyEntity
import com.arthurnagy.dexlens.core.model.Ability
import com.arthurnagy.dexlens.core.model.BaseStats
import com.arthurnagy.dexlens.core.model.EvolutionMethod
import com.arthurnagy.dexlens.core.model.EvolutionNode
import com.arthurnagy.dexlens.core.model.EvolutionTrigger
import com.arthurnagy.dexlens.core.model.Form
import com.arthurnagy.dexlens.core.model.Gender
import com.arthurnagy.dexlens.core.model.PokemonType
import com.arthurnagy.dexlens.core.model.RelativePhysicalStats
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.model.SpeciesSummary
import com.arthurnagy.dexlens.core.model.TimeOfDay
import com.arthurnagy.dexlens.core.model.TypeChart
import com.arthurnagy.dexlens.core.model.Variety

internal fun typesOf(
    primary: Int,
    secondary: Int?,
): List<PokemonType> = listOfNotNull(PokemonType.fromId(primary), secondary?.let(PokemonType::fromId))

internal fun SpeciesSummaryRow.toModel() = SpeciesSummary(SpeciesId(id), name, typesOf(typePrimary, typeSecondary))

internal fun ChainSpeciesRow.toSummary() = SpeciesSummary(SpeciesId(id), name, typesOf(typePrimary, typeSecondary))

internal fun VarietyAbilityRow.toModel() = Ability(name = name, shortEffect = shortEffect, effect = effect, isHidden = isHidden)

internal fun FormEntity.toModel() = Form(id = id, name = name, isDefault = isDefault, isBattleOnly = isBattleOnly, isMega = isMega)

internal fun VarietyEntity.toModel(
    abilities: List<Ability>,
    forms: List<Form>,
) = Variety(
    id = id,
    name = name,
    isDefault = isDefault,
    heightDecimetres = height,
    weightHectograms = weight,
    types = typesOf(typePrimary, typeSecondary),
    stats = BaseStats(hp, attack, defense, specialAttack, specialDefense, speed),
    abilities = abilities,
    forms = forms,
)

internal fun List<TypeEfficacyEntity>.toTypeChart() = TypeChart(
    associate { (PokemonType.fromId(it.attackingType) to PokemonType.fromId(it.defendingType)) to it.factor },
)

internal fun EvolutionEntity.toMethod(speciesNames: Map<Int, String>) = EvolutionMethod(
    trigger = triggerOf(trigger),
    minLevel = minLevel,
    item = item,
    heldItem = heldItem,
    timeOfDay = timeOfDay?.toTimeOfDay(),
    knownMove = knownMove,
    knownMoveType = knownMoveType?.let(PokemonType::fromId),
    minHappiness = minHappiness,
    minBeauty = minBeauty,
    minAffection = minAffection,
    relativePhysicalStats = relativePhysicalStats?.toRelativeStats(),
    partySpecies = partySpeciesId?.let(speciesNames::get),
    partyType = partyType?.let(PokemonType::fromId),
    tradeSpecies = tradeSpeciesId?.let(speciesNames::get),
    location = location,
    region = region,
    gender = gender?.toGender(),
    needsRain = needsRain,
    turnUpsideDown = turnUpsideDown,
    usedMove = usedMove,
    minMoveCount = minMoveCount,
    minSteps = minSteps,
    minDamageTaken = minDamageTaken,
)

/** Builds the tree from its root: the chain member with no parent inside the chain. */
internal fun buildEvolutionTree(
    species: List<ChainSpeciesRow>,
    evolutions: List<EvolutionEntity>,
    speciesNames: Map<Int, String>,
): EvolutionNode {
    val ids = species.map { it.id }.toSet()
    val children = species.groupBy { it.evolvesFromSpeciesId }
    val methods = evolutions.groupBy { it.speciesId }

    fun node(row: ChainSpeciesRow): EvolutionNode = EvolutionNode(
        species = row.toSummary(),
        methods = methods[row.id].orEmpty().map { it.toMethod(speciesNames) },
        children = children[row.id].orEmpty().map(::node),
    )

    return node(species.first { it.evolvesFromSpeciesId == null || it.evolvesFromSpeciesId !in ids })
}

private fun triggerOf(identifier: String): EvolutionTrigger = when (identifier) {
    "level-up" -> EvolutionTrigger.LEVEL_UP
    "trade" -> EvolutionTrigger.TRADE
    "use-item" -> EvolutionTrigger.USE_ITEM
    "shed" -> EvolutionTrigger.SHED
    "spin" -> EvolutionTrigger.SPIN
    "tower-of-darkness" -> EvolutionTrigger.TOWER_OF_DARKNESS
    "tower-of-waters" -> EvolutionTrigger.TOWER_OF_WATERS
    "three-critical-hits" -> EvolutionTrigger.THREE_CRITICAL_HITS
    "take-damage" -> EvolutionTrigger.TAKE_DAMAGE
    "in-battle-level-up" -> EvolutionTrigger.IN_BATTLE_LEVEL_UP
    "agile-style-move" -> EvolutionTrigger.AGILE_STYLE_MOVE
    "strong-style-move" -> EvolutionTrigger.STRONG_STYLE_MOVE
    "recoil-damage" -> EvolutionTrigger.RECOIL_DAMAGE
    "use-move" -> EvolutionTrigger.USE_MOVE
    "three-defeated-bisharp" -> EvolutionTrigger.THREE_DEFEATED_BISHARP
    "gimmighoul-coins" -> EvolutionTrigger.GIMMIGHOUL_COINS
    "meltan-candies" -> EvolutionTrigger.MELTAN_CANDIES
    else -> EvolutionTrigger.OTHER
}

private fun String.toTimeOfDay(): TimeOfDay? = when (this) {
    "day" -> TimeOfDay.DAY
    "night" -> TimeOfDay.NIGHT
    "dusk" -> TimeOfDay.DUSK
    else -> null
}

private fun Int.toGender(): Gender? = when (this) {
    FEMALE_ID -> Gender.FEMALE
    MALE_ID -> Gender.MALE
    else -> null
}

private fun Int.toRelativeStats(): RelativePhysicalStats? = when {
    this > 0 -> RelativePhysicalStats.ATTACK_HIGHER
    this < 0 -> RelativePhysicalStats.DEFENSE_HIGHER
    else -> RelativePhysicalStats.EQUAL
}

private const val FEMALE_ID = 1
private const val MALE_ID = 2
