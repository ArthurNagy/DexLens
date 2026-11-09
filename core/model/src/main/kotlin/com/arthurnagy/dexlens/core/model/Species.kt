package com.arthurnagy.dexlens.core.model

/** A National Pokédex number. */
@JvmInline
value class SpeciesId(
    val value: Int,
)

/** What a list row needs. [types] always holds one or two types, primary first. */
data class SpeciesSummary(
    val id: SpeciesId,
    val name: String,
    val types: List<PokemonType>,
)

/** A search result. [matchedName] is set when the query matched a name other than the displayed one. */
data class SpeciesSearchResult(
    val summary: SpeciesSummary,
    val matchedName: String?,
)

/** A species name found by the search index, in whichever language it matched. */
data class SpeciesNameHit(
    val speciesId: SpeciesId,
    val name: String,
)

data class LocalizedName(
    val language: String,
    val name: String,
)

data class SpeciesDetail(
    val summary: SpeciesSummary,
    val genus: String?,
    val generation: Int,
    val genderRatio: GenderRatio,
    val eggGroups: List<String>,
    val varieties: List<Variety>,
    val evolutionLine: EvolutionNode,
    val otherNames: List<LocalizedName>,
)

/** [femaleEighths] is null for genderless species. */
data class GenderRatio(
    val femaleEighths: Int?,
)

/** A battle-distinct variety of a species: its own types, stats, abilities and measurements. */
data class Variety(
    val id: Int,
    val name: String,
    val isDefault: Boolean,
    val heightDecimetres: Int,
    val weightHectograms: Int,
    val types: List<PokemonType>,
    val stats: BaseStats,
    val abilities: List<Ability>,
    val forms: List<Form>,
)

data class Form(
    val id: Int,
    val name: String,
    val isDefault: Boolean,
    val isBattleOnly: Boolean,
    val isMega: Boolean,
)

data class BaseStats(
    val hp: Int,
    val attack: Int,
    val defense: Int,
    val specialAttack: Int,
    val specialDefense: Int,
    val speed: Int,
) {
    val total: Int get() = hp + attack + defense + specialAttack + specialDefense + speed
}

data class Ability(
    val name: String,
    val shortEffect: String?,
    val effect: String?,
    val isHidden: Boolean,
)
