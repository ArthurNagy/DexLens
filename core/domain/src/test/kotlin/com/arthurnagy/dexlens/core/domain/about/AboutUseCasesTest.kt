package com.arthurnagy.dexlens.core.domain.about

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.testing.FakeAppInfoRepository
import com.arthurnagy.dexlens.core.testing.FakeOpenSourceLibraryRepository
import com.arthurnagy.dexlens.core.testing.LiteRtLibrary
import com.arthurnagy.dexlens.core.testing.SampleLibraryDetails
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AboutUseCasesTest {
    private val libraries = FakeOpenSourceLibraryRepository()

    @Test
    fun theLibraryListPassesThrough() = runTest {
        assertEquals(Ok(SampleLibraryDetails.map { it.library }), GetOpenSourceLibrariesUseCase(libraries)())
    }

    @Test
    fun aListFailurePassesThrough() = runTest {
        libraries.error = DomainError.Unknown("broken")

        assertEquals(Err(DomainError.Unknown("broken")), GetOpenSourceLibrariesUseCase(libraries)())
    }

    @Test
    fun aKnownLibraryPassesThrough() = runTest {
        assertEquals(Ok(LiteRtLibrary), GetOpenSourceLibraryUseCase(libraries)(LiteRtLibrary.library.id))
    }

    @Test
    fun anUnknownLibraryIsNotFound() = runTest {
        assertEquals(Err(DomainError.NotFound), GetOpenSourceLibraryUseCase(libraries)("org.example:missing"))
    }

    @Test
    fun theVersionPassesThrough() = runTest {
        assertEquals(Ok("1.2.3"), GetAppVersionUseCase(FakeAppInfoRepository(version = "1.2.3"))())
    }

    @Test
    fun aVersionFailurePassesThrough() = runTest {
        val repository = FakeAppInfoRepository().apply { error = DomainError.Unknown(null) }

        assertEquals(Err(DomainError.Unknown(null)), GetAppVersionUseCase(repository)())
    }
}
