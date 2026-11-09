package com.arthurnagy.dexlens.core.domain.content

import com.arthurnagy.dexlens.core.domain.UseCase
import javax.inject.Inject

class CancelContentDownloadUseCase
    @Inject
    constructor(
        private val repository: ContentPackRepository,
    ) : UseCase {
        operator fun invoke() = repository.cancel()
    }
