package com.arthurnagy.dexlens.core.model

/**
 * A game pair, such as Scarlet / Violet: the unit learnsets are recorded in. [id] is PokeAPI's version group, and
 * [versions] are its games' names in the display language.
 */
data class Game(
    val id: Int,
    val versions: List<String>,
    /** False when the bundled data has no wild locations for any Pokémon in this game. */
    val hasLocationData: Boolean,
)

/**
 * [level] 0 means the move is learnt on evolving. [type] is null for the old "???" type, such as Curse's before
 * Generation 5. [power] is null for moves that deal no direct damage.
 */
data class LevelUpMove(
    val level: Int,
    val name: String,
    val type: PokemonType?,
    val power: Int?,
)

/** Where a Pokémon is found in the wild. [onlyIn] names the versions it is limited to, and is empty when it is in all of them. */
data class WildEncounter(
    val location: String,
    val method: EncounterMethod,
    val minLevel: Int,
    val maxLevel: Int,
    val onlyIn: List<String>,
)

/** How a Pokémon is found: PokeAPI's [identifier], such as `surf`, and its English description as a fallback [name]. */
data class EncounterMethod(
    val identifier: String,
    val name: String,
)

data class GameDetails(
    val game: Game,
    val moves: List<LevelUpMove>,
    val encounters: List<WildEncounter>,
)

/** The games with data for a variety, newest first, and the details of the chosen one. [details] is null when [games] is empty. */
data class InGame(
    val games: List<Game>,
    val details: GameDetails?,
)
