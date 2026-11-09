package com.arthurnagy.dexlens.core.domain.content

import com.arthurnagy.dexlens.core.domain.UseCase
import com.arthurnagy.dexlens.core.model.ArtworkLocation
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveArtworkUseCase
    @Inject
    constructor(
        private val repository: ContentPackRepository,
    ) : UseCase {
        operator fun invoke(): Flow<ArtworkLocation> = repository.observeArtwork()
    }
