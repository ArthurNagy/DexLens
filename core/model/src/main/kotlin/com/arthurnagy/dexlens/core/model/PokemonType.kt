package com.arthurnagy.dexlens.core.model

/** The 18 battle types. [id] is the id used in the bundled database. */
enum class PokemonType(
    val id: Int,
) {
    NORMAL(1),
    FIGHTING(2),
    FLYING(3),
    POISON(4),
    GROUND(5),
    ROCK(6),
    BUG(7),
    GHOST(8),
    STEEL(9),
    FIRE(10),
    WATER(11),
    GRASS(12),
    ELECTRIC(13),
    PSYCHIC(14),
    ICE(15),
    DRAGON(16),
    DARK(17),
    FAIRY(18),
    ;

    companion object {
        private val byId = entries.associateBy { it.id }

        fun fromId(id: Int): PokemonType = byId.getValue(id)
    }
}

/** How much a move of [type] hurts: 0, 0.25, 0.5, 2 or 4. */
data class TypeEffectiveness(
    val type: PokemonType,
    val multiplier: Double,
)

data class TypeMatchups(
    val weaknesses: List<TypeEffectiveness>,
    val resistances: List<TypeEffectiveness>,
    val immunities: List<PokemonType>,
)

/** Damage percent for each attacking and defending type pair. */
data class TypeChart(
    val factors: Map<Pair<PokemonType, PokemonType>, Int>,
)
