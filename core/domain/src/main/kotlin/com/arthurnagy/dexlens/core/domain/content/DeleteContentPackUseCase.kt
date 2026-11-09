package com.arthurnagy.dexlens.core.domain.content

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.UseCase
import com.github.michaelbull.result.Result
import javax.inject.Inject

class DeleteContentPackUseCase
    @Inject
    constructor(
        private val repository: ContentPackRepository,
    ) : UseCase {
        suspend operator fun invoke(): Result<Unit, DomainError> = repository.delete()
    }
