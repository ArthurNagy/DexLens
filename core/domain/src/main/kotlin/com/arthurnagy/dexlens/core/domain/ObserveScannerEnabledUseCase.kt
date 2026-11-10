package com.arthurnagy.dexlens.core.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/** Whether the home screen has a scanner, as it changes. */
class ObserveScannerEnabledUseCase
    @Inject
    constructor(
        private val repository: UserPreferencesRepository,
    ) : UseCase {
        operator fun invoke(): Flow<Boolean> = repository.scannerEnabled.distinctUntilChanged()
    }
