package com.arthurnagy.dexlens.feature.entry.impl

import androidx.compose.runtime.Immutable
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.species.calculateTypeMatchups
import com.arthurnagy.dexlens.core.model.ArtworkLocation
import com.arthurnagy.dexlens.core.model.Game
import com.arthurnagy.dexlens.core.model.GameDetails
import com.arthurnagy.dexlens.core.model.InGame
import com.arthurnagy.dexlens.core.model.SpeciesDetail
import com.arthurnagy.dexlens.core.model.TypeChart
import com.arthurnagy.dexlens.core.model.TypeMatchups
import com.arthurnagy.dexlens.core.model.Variety

/** UI state for [EntryScreen]: one of three exclusive page states. */
internal sealed interface EntryState {
    data object Loading : EntryState

    data class Failed(
        val error: DomainError,
    ) : EntryState

    /**
     * [selected] picks which variety's stats, types, matchups and in-game details are shown. [description] is the newest
     * downloaded Pokédex text, if any. [inGame] is null until it loads, and when the variety appears in no game.
     */
    @Immutable
    data class Loaded(
        val detail: SpeciesDetail,
        val chart: TypeChart,
        val selected: Variety,
        val matchups: TypeMatchups,
        val artwork: ArtworkLocation,
        val description: String?,
        val inGame: InGameState?,
    ) : EntryState {
        fun select(variety: Variety): Loaded = copy(selected = variety, matchups = calculateTypeMatchups(chart, variety.types))
    }

    companion object {
        val Initial: EntryState = Loading

        fun loaded(
            detail: SpeciesDetail,
            chart: TypeChart,
            artwork: ArtworkLocation,
            description: String?,
        ): Loaded {
            val variety = detail.varieties.first { it.isDefault }
            return Loaded(detail, chart, variety, calculateTypeMatchups(chart, variety.types), artwork, description, inGame = null)
        }
    }
}

/** The "In game" section. [details] can still be the previous game's for a moment after a new [selected] game is picked. */
@Immutable
internal data class InGameState(
    val games: List<Game>,
    val selected: Game,
    val details: GameDetails,
)

internal fun InGame.toState(): InGameState? = details?.let { InGameState(games, it.game, it) }
