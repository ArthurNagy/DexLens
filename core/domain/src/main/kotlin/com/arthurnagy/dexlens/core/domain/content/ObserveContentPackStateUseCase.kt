package com.arthurnagy.dexlens.core.domain.content

import com.arthurnagy.dexlens.core.domain.UseCase
import com.arthurnagy.dexlens.core.model.ContentPackState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveContentPackStateUseCase
    @Inject
    constructor(
        private val repository: ContentPackRepository,
    ) : UseCase {
        operator fun invoke(): Flow<ContentPackState> = repository.observeState()
    }
