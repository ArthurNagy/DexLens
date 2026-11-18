package com.arthurnagy.dexlens.feature.pokedex.impl

import androidx.compose.runtime.Immutable
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.model.ArtworkLocation
import com.arthurnagy.dexlens.core.model.GenerationStart
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
    /** Each generation's first species, or empty until loaded (and if they can't be read). */
    val generations: List<GenerationStart>,
    /**
     * The query [results] answer. It lags [query] until the new answer arrives, so a just-cleared query can still show the
     * previous query's results under a blank field.
     */
    val answeredQuery: String?,
) {
    /** A scan match is showing in place of the full list. Typed text always takes over from one. */
    val matchListed: Boolean get() = query.isBlank() && scan.match.isNotEmpty()

    /** What the list shows: the match, or otherwise the answer to [query]. */
    val listed: List<SpeciesSearchResult>
        get() = if (matchListed) scan.match.map { SpeciesSearchResult(it, matchedName = null) } else results

    /** Every species in Pokédex order is listed, loaded and answering a blank query: the only list the generation thumb fits. */
    val fullListShown: Boolean
        get() = !loading && error == null && !matchListed && query.isBlank() && answeredQuery?.isBlank() == true && results.isNotEmpty()

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
                generations = emptyList(),
                answeredQuery = null,
            )
    }
}
