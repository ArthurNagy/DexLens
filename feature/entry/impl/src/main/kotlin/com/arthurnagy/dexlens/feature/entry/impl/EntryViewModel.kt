package com.arthurnagy.dexlens.feature.entry.impl

import androidx.lifecycle.ViewModel
import com.arthurnagy.dexlens.core.domain.content.ObserveArtworkUseCase
import com.arthurnagy.dexlens.core.domain.content.ObserveFlavorEntriesUseCase
import com.arthurnagy.dexlens.core.domain.game.GetInGameDetailsUseCase
import com.arthurnagy.dexlens.core.domain.game.SetPreferredGameUseCase
import com.arthurnagy.dexlens.core.domain.species.GetSpeciesDetailUseCase
import com.arthurnagy.dexlens.core.domain.species.GetTypeChartUseCase
import com.arthurnagy.dexlens.core.model.SpeciesDetail
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.model.TypeChart
import com.arthurnagy.dexlens.feature.entry.api.EntryKey
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.syntax.Syntax
import org.orbitmvi.orbit.viewmodel.orbitContainer

/**
 * Loads one species and its type chart, then the moves and locations for one game. Selecting a variety recalculates
 * matchups for its types and reloads the in-game section.
 */
@HiltViewModel(assistedFactory = EntryViewModel.Factory::class)
internal class EntryViewModel
    @AssistedInject
    constructor(
        @Assisted private val key: EntryKey,
        private val getSpeciesDetail: GetSpeciesDetailUseCase,
        private val getTypeChart: GetTypeChartUseCase,
        private val observeArtwork: ObserveArtworkUseCase,
        private val observeFlavorEntries: ObserveFlavorEntriesUseCase,
        private val getInGameDetails: GetInGameDetailsUseCase,
        private val setPreferredGame: SetPreferredGameUseCase,
    ) : ViewModel(),
        OrbitContainerHost<EntryState, EntryState, Nothing> {
        override val container = orbitContainer<EntryState, Nothing>(EntryState.Initial) {
            load()
        }

        fun load() = intent {
            getSpeciesDetail(SpeciesId(key.speciesId))
                .andThen { detail -> getTypeChart().map { chart -> detail to chart } }
                .onOk { (detail, chart) -> showWithDownloadedContent(detail, chart) }
                .onErr { error -> reduce { EntryState.Failed(error) } }
        }

        /** The species data never changes, but downloaded art and text can arrive or be deleted while the entry is open. */
        private suspend fun Syntax<EntryState, Nothing>.showWithDownloadedContent(
            detail: SpeciesDetail,
            chart: TypeChart,
        ) {
            combine(observeArtwork(), observeFlavorEntries(detail.summary.id)) { artwork, flavor -> artwork to flavor.firstOrNull()?.text }
                .collect { (artwork, description) ->
                    val first = state !is EntryState.Loaded
                    reduce {
                        (state as? EntryState.Loaded)?.copy(artwork = artwork, description = description)
                            ?: EntryState.loaded(detail, chart, artwork, description)
                    }
                    (state as? EntryState.Loaded)?.takeIf { first }?.let { loadInGame(it.selected.id, requestedGameId = null) }
                }
        }

        fun onSelectVariety(varietyId: Int) = intent {
            val loaded = state as? EntryState.Loaded ?: return@intent
            val variety = loaded.detail.varieties.firstOrNull { it.id == varietyId } ?: return@intent
            reduce { loaded.select(variety) }
            loadInGame(varietyId, loaded.inGame?.selected?.id)
        }

        /** Shows the picked game at once and remembers it for other entries; its moves and locations follow. */
        fun onSelectGame(gameId: Int) = intent {
            val loaded = state as? EntryState.Loaded ?: return@intent
            val inGame = loaded.inGame ?: return@intent
            val game = inGame.games.firstOrNull { it.id == gameId } ?: return@intent
            reduce { loaded.copy(inGame = inGame.copy(selected = game)) }
            setPreferredGame(gameId)
            loadInGame(loaded.selected.id, gameId)
        }

        /** Drops the result when the user has since picked another variety or game. A failure leaves the section as it was. */
        private fun loadInGame(
            varietyId: Int,
            requestedGameId: Int?,
        ) = intent {
            getInGameDetails(varietyId, requestedGameId).onOk { inGame ->
                reduce {
                    val loaded = state as? EntryState.Loaded
                    val pickedGame = loaded?.inGame?.selected?.id
                    if (loaded != null && loaded.selected.id == varietyId &&
                        (requestedGameId == null || pickedGame in listOf(null, requestedGameId))
                    ) {
                        loaded.copy(inGame = inGame.toState())
                    } else {
                        state
                    }
                }
            }
        }

        @AssistedFactory
        interface Factory {
            fun create(key: EntryKey): EntryViewModel
        }
    }
