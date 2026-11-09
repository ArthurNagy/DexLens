package com.arthurnagy.dexlens.feature.entry.impl

import androidx.compose.runtime.Immutable
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.species.calculateTypeMatchups
import com.arthurnagy.dexlens.core.model.ArtworkLocation
import com.arthurnagy.dexlens.core.model.FlavorEntry
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

    /** [selected] picks which variety's stats, types and matchups are shown. */
    @Immutable
    data class Loaded(
        val detail: SpeciesDetail,
        val chart: TypeChart,
        val selected: Variety,
        val matchups: TypeMatchups,
        val artwork: ArtworkLocation,
        val flavor: List<FlavorEntry>,
    ) : EntryState {
        fun select(variety: Variety): Loaded = copy(selected = variety, matchups = calculateTypeMatchups(chart, variety.types))
    }

    companion object {
        val Initial: EntryState = Loading

        fun loaded(
            detail: SpeciesDetail,
            chart: TypeChart,
            artwork: ArtworkLocation,
            flavor: List<FlavorEntry>,
        ): Loaded {
            val variety = detail.varieties.first { it.isDefault }
            return Loaded(detail, chart, variety, calculateTypeMatchups(chart, variety.types), artwork, flavor)
        }
    }
}
