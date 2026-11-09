package com.arthurnagy.dexlens.core.domain.content

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.UseCase
import com.arthurnagy.dexlens.core.domain.UserPreferencesRepository
import com.arthurnagy.dexlens.core.model.ArtworkLocation
import com.arthurnagy.dexlens.core.model.ContentPackState
import com.arthurnagy.dexlens.core.model.FlavorEntry
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveContentPackStateUseCase
    @Inject
    constructor(
        private val repository: ContentPackRepository,
    ) : UseCase {
        operator fun invoke(): Flow<ContentPackState> = repository.observeState()
    }

class ObserveArtworkUseCase
    @Inject
    constructor(
        private val repository: ContentPackRepository,
    ) : UseCase {
        operator fun invoke(): Flow<ArtworkLocation> = repository.observeArtwork()
    }

class ObserveContentOfferUseCase
    @Inject
    constructor(
        private val repository: UserPreferencesRepository,
    ) : UseCase {
        /** Emits true when the first-launch offer still has to be shown. */
        operator fun invoke(): Flow<Boolean> = repository.contentOfferShown.map { shown -> !shown }
    }

class MarkContentOfferShownUseCase
    @Inject
    constructor(
        private val repository: UserPreferencesRepository,
    ) : UseCase {
        suspend operator fun invoke(): Result<Unit, DomainError> = repository.setContentOfferShown(shown = true)
    }

class StartContentDownloadUseCase
    @Inject
    constructor(
        private val repository: ContentPackRepository,
    ) : UseCase {
        operator fun invoke() = repository.start()
    }

class CancelContentDownloadUseCase
    @Inject
    constructor(
        private val repository: ContentPackRepository,
    ) : UseCase {
        operator fun invoke() = repository.cancel()
    }

class DeleteContentPackUseCase
    @Inject
    constructor(
        private val repository: ContentPackRepository,
    ) : UseCase {
        suspend operator fun invoke(): Result<Unit, DomainError> = repository.delete()
    }

class GetFlavorEntriesUseCase
    @Inject
    constructor(
        private val repository: FlavorTextRepository,
    ) : UseCase {
        suspend operator fun invoke(id: SpeciesId): Result<List<FlavorEntry>, DomainError> = repository.flavorEntries(id)
    }
