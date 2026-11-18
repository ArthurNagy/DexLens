package com.arthurnagy.dexlens.core.domain.connectivity

import com.arthurnagy.dexlens.core.domain.UseCase
import com.arthurnagy.dexlens.core.model.Connectivity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Whether the device is online or offline, as it changes. */
class ObserveConnectivityUseCase
    @Inject
    constructor(
        private val repository: ConnectivityRepository,
    ) : UseCase {
        operator fun invoke(): Flow<Connectivity> = repository.observeConnectivity()
    }
