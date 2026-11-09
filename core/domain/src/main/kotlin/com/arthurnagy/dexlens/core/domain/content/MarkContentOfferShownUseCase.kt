package com.arthurnagy.dexlens.core.domain.content

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.UseCase
import com.arthurnagy.dexlens.core.domain.UserPreferencesRepository
import com.github.michaelbull.result.Result
import javax.inject.Inject

class MarkContentOfferShownUseCase
    @Inject
    constructor(
        private val repository: UserPreferencesRepository,
    ) : UseCase {
        suspend operator fun invoke(): Result<Unit, DomainError> = repository.setContentOfferShown(shown = true)
    }
