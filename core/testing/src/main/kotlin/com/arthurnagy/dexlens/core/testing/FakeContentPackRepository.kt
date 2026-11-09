package com.arthurnagy.dexlens.core.testing

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.content.ContentPackRepository
import com.arthurnagy.dexlens.core.model.ArtworkLocation
import com.arthurnagy.dexlens.core.model.ContentPackState
import com.arthurnagy.dexlens.core.model.FlavorEntry
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeContentPackRepository(
    state: ContentPackState = ContentPackState.NotDownloaded,
    artwork: ArtworkLocation = ArtworkLocation(null),
) : ContentPackRepository {
    val state = MutableStateFlow(state)
    val artwork = MutableStateFlow(artwork)
    var started = 0
        private set
    var cancelled = 0
        private set
    var deleted = 0
        private set

    override fun observeState(): Flow<ContentPackState> = state

    override fun observeArtwork(): Flow<ArtworkLocation> = artwork

    override fun start() {
        started++
    }

    override fun cancel() {
        cancelled++
    }

    override suspend fun delete(): Result<Unit, DomainError> {
        deleted++
        state.value = ContentPackState.NotDownloaded
        return Ok(Unit)
    }
}
