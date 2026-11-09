package com.arthurnagy.dexlens.core.model

/** Progress of the optional artwork and flavor text download. */
sealed interface ContentPackState {
    data object NotDownloaded : ContentPackState

    data class Downloading(
        val completed: Int,
        val total: Int,
    ) : ContentPackState

    /** Stopped before finishing: finished items are kept and the download can resume. */
    data class Interrupted(
        val completed: Int,
        val total: Int,
    ) : ContentPackState

    data object Ready : ContentPackState
}

/** Where downloaded pictures live. [rootPath] is null until something has been downloaded. */
data class ArtworkLocation(
    val rootPath: String?,
) {
    fun sprite(id: SpeciesId): String? = rootPath?.let { "$it/sprites/${id.value}.webp" }

    fun artwork(id: SpeciesId): String? = rootPath?.let { "$it/artwork/${id.value}.webp" }
}

/** One game's description of a species. Identical texts from several games are grouped. */
data class FlavorEntry(
    val text: String,
    val games: List<String>,
)
