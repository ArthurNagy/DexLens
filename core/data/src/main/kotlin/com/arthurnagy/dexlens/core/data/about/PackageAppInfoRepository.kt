package com.arthurnagy.dexlens.core.data.about

import android.content.Context
import com.arthurnagy.dexlens.core.data.catchStorage
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.about.AppInfoRepository
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** The installed app's facts, from [android.content.pm.PackageManager]. */
internal class PackageAppInfoRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : AppInfoRepository {
        override suspend fun versionName(): Result<String, DomainError> = catchStorage {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.andThen { version -> if (version == null) Err(DomainError.Unknown("No version name")) else Ok(version) }
    }
