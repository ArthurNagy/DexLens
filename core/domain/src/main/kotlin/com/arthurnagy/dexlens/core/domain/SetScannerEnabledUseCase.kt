package com.arthurnagy.dexlens.core.domain

import com.github.michaelbull.result.Result
import javax.inject.Inject

/** Stores whether the home screen has a scanner. */
class SetScannerEnabledUseCase
    @Inject
    constructor(
        private val repository: UserPreferencesRepository,
    ) : UseCase {
        suspend operator fun invoke(enabled: Boolean): Result<Unit, DomainError> = repository.setScannerEnabled(enabled)
    }
