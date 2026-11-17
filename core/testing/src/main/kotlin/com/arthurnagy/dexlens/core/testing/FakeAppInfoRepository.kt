package com.arthurnagy.dexlens.core.testing

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.about.AppInfoRepository
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

/** In-memory [AppInfoRepository]: answers [version], or fails with [error] when set. */
class FakeAppInfoRepository(
    var version: String = "0.1.0",
) : AppInfoRepository {
    var error: DomainError? = null

    override suspend fun versionName(): Result<String, DomainError> = error?.let { Err(it) } ?: Ok(version)
}
