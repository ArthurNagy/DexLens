package com.arthurnagy.dexlens.feature.pokedex.impl

import androidx.compose.runtime.Immutable
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.model.ArtworkLocation
import com.arthurnagy.dexlens.core.model.SpeciesSearchResult
import com.arthurnagy.dexlens.feature.pokedex.impl.scanner.ScanState

/** UI state for [PokedexScreen]. [results] answer [query], or the query before it until the new answer arrives. */
@Immutable
internal data class PokedexState(
    val loading: Boolean,
    val error: DomainError?,
    val query: String,
    val results: List<SpeciesSearchResult>,
    val artwork: ArtworkLocation,
    /** True once on first launch, until the screen has shown the download offer. */
    val pendingOffer: Boolean,
    /** The Settings switch. Null until it has been read, so the camera can't start for someone who turned it off. */
    val scannerEnabled: Boolean?,
    val scan: ScanState,
) {
    /** A scan match is showing in place of the full list. Typed text always takes over from one. */
    val matchListed: Boolean get() = query.isBlank() && scan.match.isNotEmpty()

    /** What the list shows: the match, or otherwise the answer to [query]. */
    val listed: List<SpeciesSearchResult>
        get() = if (matchListed) scan.match.map { SpeciesSearchResult(it, matchedName = null) } else results

    companion object {
        val Initial =
            PokedexState(
                loading = true,
                error = null,
                query = "",
                results = emptyList(),
                artwork = ArtworkLocation(null),
                pendingOffer = false,
                scannerEnabled = null,
                scan = ScanState.Initial,
            )
    }
}
