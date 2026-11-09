package com.arthurnagy.dexlens.core.testing

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.species.TypeChartRepository
import com.arthurnagy.dexlens.core.model.PokemonType
import com.arthurnagy.dexlens.core.model.TypeChart
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

class FakeTypeChartRepository(
    var chart: TypeChart = TypeChart(emptyMap()),
) : TypeChartRepository {
    var nextError: DomainError? = null

    override suspend fun typeChart(): Result<TypeChart, DomainError> {
        nextError?.let {
            nextError = null
            return Err(it)
        }
        return Ok(chart)
    }
}

/** The chart pairs the type-matchup and entry tests rely on. Unlisted pairs are neutral. */
fun sampleTypeChart(): TypeChart = TypeChart(
    buildMap {
        operator fun MutableMap<Pair<PokemonType, PokemonType>, Int>.set(
            attacker: PokemonType,
            defender: PokemonType,
            factor: Int,
        ) = put(attacker to defender, factor)

        this[PokemonType.FIRE, PokemonType.GRASS] = 200
        this[PokemonType.FIRE, PokemonType.BUG] = 200
        this[PokemonType.FIRE, PokemonType.STEEL] = 200
        this[PokemonType.ICE, PokemonType.GRASS] = 200
        this[PokemonType.FLYING, PokemonType.GRASS] = 200
        this[PokemonType.PSYCHIC, PokemonType.POISON] = 200
        this[PokemonType.GROUND, PokemonType.POISON] = 200
        this[PokemonType.WATER, PokemonType.GRASS] = 50
        this[PokemonType.ELECTRIC, PokemonType.GRASS] = 50
        this[PokemonType.GRASS, PokemonType.GRASS] = 50
        this[PokemonType.GRASS, PokemonType.POISON] = 50
        this[PokemonType.FIGHTING, PokemonType.POISON] = 50
        this[PokemonType.POISON, PokemonType.POISON] = 50
        this[PokemonType.NORMAL, PokemonType.GHOST] = 0
        this[PokemonType.FIGHTING, PokemonType.GHOST] = 0
    },
)
