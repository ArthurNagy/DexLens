package com.arthurnagy.dexlens.core.domain

import com.github.michaelbull.result.Result
import javax.inject.Inject

/** Stores whether the scanner shows the picture signal's scores. */
class SetRecognitionScoresShownUseCase
    @Inject
    constructor(
        private val repository: UserPreferencesRepository,
    ) : UseCase {
        suspend operator fun invoke(shown: Boolean): Result<Unit, DomainError> = repository.setRecognitionScoresShown(shown)
    }
