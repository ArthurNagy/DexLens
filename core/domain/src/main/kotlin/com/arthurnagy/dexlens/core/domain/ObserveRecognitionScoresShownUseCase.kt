package com.arthurnagy.dexlens.core.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/** Whether the scanner shows the picture signal's scores, as it changes. */
class ObserveRecognitionScoresShownUseCase
    @Inject
    constructor(
        private val repository: UserPreferencesRepository,
    ) : UseCase {
        operator fun invoke(): Flow<Boolean> = repository.recognitionScoresShown.distinctUntilChanged()
    }
