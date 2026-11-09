package com.arthurnagy.dexlens.feature.entry.impl

import androidx.lifecycle.ViewModel
import com.arthurnagy.dexlens.core.domain.content.ObserveArtworkUseCase
import com.arthurnagy.dexlens.core.domain.content.ObserveFlavorEntriesUseCase
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

/** Loads one species and its type chart. Selecting a variety recalculates matchups for its types. */
@HiltViewModel(assistedFactory = EntryViewModel.Factory::class)
internal class EntryViewModel
    @AssistedInject
    constructor(
        @Assisted private val key: EntryKey,
        private val getSpeciesDetail: GetSpeciesDetailUseCase,
        private val getTypeChart: GetTypeChartUseCase,
        private val observeArtwork: ObserveArtworkUseCase,
        private val observeFlavorEntries: ObserveFlavorEntriesUseCase,
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
            combine(observeArtwork(), observeFlavorEntries(detail.summary.id)) { artwork, flavor -> artwork to flavor }
                .collect { (artwork, flavor) ->
                    reduce {
                        (state as? EntryState.Loaded)?.copy(artwork = artwork, flavor = flavor)
                            ?: EntryState.loaded(detail, chart, artwork, flavor)
                    }
                }
        }

        fun onSelectVariety(varietyId: Int) = intent {
            val loaded = state as? EntryState.Loaded ?: return@intent
            val variety = loaded.detail.varieties.firstOrNull { it.id == varietyId } ?: return@intent
            reduce { loaded.select(variety) }
        }

        @AssistedFactory
        interface Factory {
            fun create(key: EntryKey): EntryViewModel
        }
    }
