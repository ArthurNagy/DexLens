package com.arthurnagy.dexlens.core.domain.content

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.model.ArtworkLocation
import com.arthurnagy.dexlens.core.model.ContentPackState
import com.arthurnagy.dexlens.core.model.FlavorEntry
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow

/** The optional download of artwork and flavor text. Nothing here touches the network until [start] is called. */
interface ContentPackRepository {
    fun observeState(): Flow<ContentPackState>

    fun observeArtwork(): Flow<ArtworkLocation>

    /** Starts or resumes the download. */
    fun start()

    fun cancel()

    suspend fun delete(): Result<Unit, DomainError>
}

/** Downloaded in-game Pokédex text. Empty until the optional download has run. */
interface FlavorTextRepository {
    suspend fun flavorEntries(id: SpeciesId): Result<List<FlavorEntry>, DomainError>
}

/** Sizes measured from a full download on a Pixel 6, so the offer can state them before any transfer. */
object ContentPackSizes {
    const val DOWNLOAD_MB = 140
    const val STORED_MB = 25
}
