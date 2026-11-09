package com.arthurnagy.dexlens.core.model

/** One species in an evolution tree. [methods] says how its parent becomes it, and is empty at the root. */
data class EvolutionNode(
    val species: SpeciesSummary,
    val methods: List<EvolutionMethod>,
    val children: List<EvolutionNode>,
)

enum class EvolutionTrigger {
    LEVEL_UP,
    TRADE,
    USE_ITEM,
    SHED,
    SPIN,
    TOWER_OF_DARKNESS,
    TOWER_OF_WATERS,
    THREE_CRITICAL_HITS,
    TAKE_DAMAGE,
    IN_BATTLE_LEVEL_UP,
    AGILE_STYLE_MOVE,
    STRONG_STYLE_MOVE,
    RECOIL_DAMAGE,
    USE_MOVE,
    THREE_DEFEATED_BISHARP,
    GIMMIGHOUL_COINS,
    MELTAN_CANDIES,
    OTHER,
}

enum class TimeOfDay { DAY, NIGHT, DUSK }

enum class Gender { FEMALE, MALE }

enum class RelativePhysicalStats { ATTACK_HIGHER, EQUAL, DEFENSE_HIGHER }

/** Structured conditions: the entry screen words them from string resources. */
data class EvolutionMethod(
    val trigger: EvolutionTrigger,
    val minLevel: Int?,
    val item: String?,
    val heldItem: String?,
    val timeOfDay: TimeOfDay?,
    val knownMove: String?,
    val knownMoveType: PokemonType?,
    val minHappiness: Int?,
    val minBeauty: Int?,
    val minAffection: Int?,
    val relativePhysicalStats: RelativePhysicalStats?,
    val partySpecies: String?,
    val partyType: PokemonType?,
    val tradeSpecies: String?,
    val location: String?,
    val region: String?,
    val gender: Gender?,
    val needsRain: Boolean,
    val turnUpsideDown: Boolean,
    val usedMove: String?,
    val minMoveCount: Int?,
    val minSteps: Int?,
    val minDamageTaken: Int?,
)
