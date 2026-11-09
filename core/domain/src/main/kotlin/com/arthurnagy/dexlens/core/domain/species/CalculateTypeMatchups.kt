package com.arthurnagy.dexlens.core.domain.species

import com.arthurnagy.dexlens.core.model.PokemonType
import com.arthurnagy.dexlens.core.model.TypeChart
import com.arthurnagy.dexlens.core.model.TypeEffectiveness
import com.arthurnagy.dexlens.core.model.TypeMatchups

/** Multiplies the chart's factors for each attacking type against the defender's types. */
fun calculateTypeMatchups(
    chart: TypeChart,
    defenderTypes: List<PokemonType>,
): TypeMatchups {
    val multipliers = PokemonType.entries.map { attacker ->
        val multiplier = defenderTypes.fold(1.0) { product, defender ->
            product * (chart.factors[attacker to defender] ?: PERCENT) / PERCENT
        }
        TypeEffectiveness(attacker, multiplier)
    }
    return TypeMatchups(
        weaknesses = multipliers.filter { it.multiplier > 1.0 }.sortedByDescending { it.multiplier },
        resistances = multipliers.filter { it.multiplier > 0.0 && it.multiplier < 1.0 }.sortedBy { it.multiplier },
        immunities = multipliers.filter { it.multiplier == 0.0 }.map { it.type },
    )
}

private const val PERCENT = 100
