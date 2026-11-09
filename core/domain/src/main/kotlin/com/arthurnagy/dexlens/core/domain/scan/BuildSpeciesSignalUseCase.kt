package com.arthurnagy.dexlens.core.domain.scan

import com.arthurnagy.dexlens.core.domain.DispatcherProvider
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.UseCase
import com.arthurnagy.dexlens.core.domain.species.SpeciesRepository
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Builds the camera's name matcher from the bundled names. Normalising about 13,000 names is CPU work. */
class BuildSpeciesSignalUseCase
    @Inject
    constructor(
        private val repository: SpeciesRepository,
        private val dispatchers: DispatcherProvider,
    ) : UseCase {
        suspend operator fun invoke(): Result<SpeciesSignal, DomainError> = repository.scanNames().map { names ->
            withContext(dispatchers.default) { TextNameSignal(ScanNameIndex(names)) }
        }
    }
