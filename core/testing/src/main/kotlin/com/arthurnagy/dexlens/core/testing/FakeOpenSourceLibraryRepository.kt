package com.arthurnagy.dexlens.core.testing

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.about.AppInfoRepository
import com.arthurnagy.dexlens.core.domain.about.OpenSourceLibraryRepository
import com.arthurnagy.dexlens.core.model.LibraryLicense
import com.arthurnagy.dexlens.core.model.LibraryNotice
import com.arthurnagy.dexlens.core.model.OpenSourceLibrary
import com.arthurnagy.dexlens.core.model.OpenSourceLibraryDetail
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

/**
 * In-memory [OpenSourceLibraryRepository]: [details] are the bundled libraries, listed in the given order; an unknown id
 * gives [DomainError.NotFound]. Set [error] to make every call fail with it.
 */
class FakeOpenSourceLibraryRepository(
    var details: List<OpenSourceLibraryDetail> = SampleLibraryDetails,
) : OpenSourceLibraryRepository {
    var error: DomainError? = null
    val requestedIds = mutableListOf<String>()

    override suspend fun libraries(): Result<List<OpenSourceLibrary>, DomainError> = error?.let { Err(it) } ?: Ok(details.map { it.library })

    override suspend fun library(id: String): Result<OpenSourceLibraryDetail, DomainError> {
        requestedIds += id
        error?.let { return Err(it) }
        return details.firstOrNull { it.library.id == id }?.let { Ok(it) } ?: Err(DomainError.NotFound)
    }
}

/** In-memory [AppInfoRepository]: answers [version], or fails with [error] when set. */
class FakeAppInfoRepository(
    var version: String = "0.1.0",
) : AppInfoRepository {
    var error: DomainError? = null

    override suspend fun versionName(): Result<String, DomainError> = error?.let { Err(it) } ?: Ok(version)
}

val ApacheLicense = LibraryLicense(
    name = "Apache License 2.0",
    text = "Licensed under the Apache License, Version 2.0 (the \"License\");\nyou may not use this file except in compliance with the License.\n\n" +
        "Unless required by applicable law or agreed to in writing, software\ndistributed under the License is distributed on an \"AS IS\" BASIS.",
)

val MitLicense = LibraryLicense(
    name = "MIT License",
    text = "Permission is hereby granted, free of charge, to any person obtaining a copy of this software.\n\n" +
        "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND.",
)

val CoilLibrary = OpenSourceLibraryDetail(
    library = OpenSourceLibrary(id = "io.coil-kt.coil3:coil", name = "coil", version = "3.3.0", licenseNames = listOf(ApacheLicense.name)),
    copyrightHolder = "Coil Contributors",
    licenses = listOf(ApacheLicense),
    notices = emptyList(),
)

val KotlinResultLibrary = OpenSourceLibraryDetail(
    library = OpenSourceLibrary(id = "com.michael-bull.kotlin-result:kotlin-result", name = "kotlin-result", version = "2.1.0", licenseNames = listOf("ISC License")),
    copyrightHolder = "Michael Bull",
    licenses = listOf(LibraryLicense(name = "ISC License", text = "Permission to use, copy, modify, and/or distribute this software for any purpose is hereby granted.")),
    notices = emptyList(),
)

val LiteRtLibrary = OpenSourceLibraryDetail(
    library = OpenSourceLibrary(id = "com.google.ai.edge.litert:litert", name = "LiteRT", version = "1.4.0", licenseNames = listOf(ApacheLicense.name)),
    copyrightHolder = "Google LLC",
    licenses = listOf(ApacheLicense),
    notices = listOf(
        LibraryNotice(
            fileName = "THIRD_PARTY_NOTICE.txt",
            text = "abseil-cpp\n\nApache License, Version 2.0\n\nflatbuffers\n\nApache License, Version 2.0",
        ),
    ),
)

val MitLibrary = OpenSourceLibraryDetail(
    library = OpenSourceLibrary(id = "org.example:no-version", name = "Example without version", version = null, licenseNames = listOf(MitLicense.name, ApacheLicense.name)),
    copyrightHolder = null,
    licenses = listOf(MitLicense, ApacheLicense),
    notices = emptyList(),
)

/** A few bundled libraries in name order: one with an embedded notice (LiteRT) and one without a version or holder. */
val SampleLibraryDetails: List<OpenSourceLibraryDetail> = listOf(CoilLibrary, MitLibrary, KotlinResultLibrary, LiteRtLibrary)
