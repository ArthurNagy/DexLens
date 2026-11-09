package com.arthurnagy.dexlens.feature.entry.impl

import androidx.lifecycle.ViewModel
import com.arthurnagy.dexlens.core.domain.content.GetFlavorEntriesUseCase
import com.arthurnagy.dexlens.core.domain.content.ObserveArtworkUseCase
import com.arthurnagy.dexlens.core.domain.species.GetSpeciesDetailUseCase
import com.arthurnagy.dexlens.core.domain.species.GetTypeChartUseCase
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.feature.entry.api.EntryKey
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.getOr
import com.github.michaelbull.result.map
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import org.orbitmvi.orbit.OrbitContainerHost
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
        private val getFlavorEntries: GetFlavorEntriesUseCase,
    ) : ViewModel(),
        OrbitContainerHost<EntryState, EntryState, Nothing> {
        override val container = orbitContainer<EntryState, Nothing>(EntryState.Initial) {
            load()
        }

        fun load() = intent {
            getSpeciesDetail(SpeciesId(key.speciesId))
                .andThen { detail -> getTypeChart().map { chart -> detail to chart } }
                .onOk { (detail, chart) ->
                    val artwork = observeArtwork().first()
                    val flavor = getFlavorEntries(detail.summary.id).getOr(emptyList())
                    reduce { EntryState.loaded(detail, chart, artwork, flavor) }
                }
                .onErr { error -> reduce { EntryState.Failed(error) } }
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
