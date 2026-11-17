package com.arthurnagy.dexlens.core.domain.about

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.UseCase
import com.github.michaelbull.result.Result
import javax.inject.Inject

/** The installed app's version name. */
class GetAppVersionUseCase
    @Inject
    constructor(
        private val repository: AppInfoRepository,
    ) : UseCase {
        suspend operator fun invoke(): Result<String, DomainError> = repository.versionName()
    }
