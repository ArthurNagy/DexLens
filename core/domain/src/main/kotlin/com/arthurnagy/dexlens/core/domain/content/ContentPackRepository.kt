package com.arthurnagy.dexlens.core.domain.content

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.model.ArtworkLocation
import com.arthurnagy.dexlens.core.model.ContentPackState
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
