package com.arthurnagy.dexlens.core.domain.about

import com.arthurnagy.dexlens.core.domain.DomainError
import com.github.michaelbull.result.Result

/** Facts about the installed app. */
interface AppInfoRepository {
    /** The installed build's version name, such as "0.1.0". */
    suspend fun versionName(): Result<String, DomainError>
}
