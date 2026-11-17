package com.arthurnagy.dexlens.core.domain.about

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.UseCase
import com.arthurnagy.dexlens.core.model.OpenSourceLibrary
import com.github.michaelbull.result.Result
import javax.inject.Inject

/** Every third-party library bundled in the app, ordered by name. */
class GetOpenSourceLibrariesUseCase
    @Inject
    constructor(
        private val repository: OpenSourceLibraryRepository,
    ) : UseCase {
        suspend operator fun invoke(): Result<List<OpenSourceLibrary>, DomainError> = repository.libraries()
    }
