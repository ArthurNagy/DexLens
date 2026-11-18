package com.arthurnagy.dexlens.core.domain.connectivity

import com.arthurnagy.dexlens.core.domain.UseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Whether the device has a usable internet connection, as it changes. */
class ObserveInternetAvailableUseCase
    @Inject
    constructor(
        private val repository: ConnectivityRepository,
    ) : UseCase {
        operator fun invoke(): Flow<Boolean> = repository.observeInternetAvailable()
    }
