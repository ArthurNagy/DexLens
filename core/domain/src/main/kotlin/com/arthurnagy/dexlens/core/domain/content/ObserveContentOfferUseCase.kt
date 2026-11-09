package com.arthurnagy.dexlens.core.domain.content

import com.arthurnagy.dexlens.core.domain.UseCase
import com.arthurnagy.dexlens.core.domain.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveContentOfferUseCase
    @Inject
    constructor(
        private val repository: UserPreferencesRepository,
    ) : UseCase {
        /** Emits true when the first-launch offer still has to be shown. */
        operator fun invoke(): Flow<Boolean> = repository.contentOfferShown.map { shown -> !shown }
    }
