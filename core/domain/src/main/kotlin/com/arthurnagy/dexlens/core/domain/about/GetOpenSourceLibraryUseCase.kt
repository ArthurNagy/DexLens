package com.arthurnagy.dexlens.core.domain.about

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.UseCase
import com.arthurnagy.dexlens.core.model.OpenSourceLibraryDetail
import com.github.michaelbull.result.Result
import javax.inject.Inject

/** One bundled library with its licence and notice texts, or [DomainError.NotFound] when it isn't bundled. */
class GetOpenSourceLibraryUseCase
    @Inject
    constructor(
        private val repository: OpenSourceLibraryRepository,
    ) : UseCase {
        suspend operator fun invoke(id: String): Result<OpenSourceLibraryDetail, DomainError> = repository.library(id)
    }
