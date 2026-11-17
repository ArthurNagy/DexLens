package com.arthurnagy.dexlens.core.data.about

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.model.LibraryLicense
import com.arthurnagy.dexlens.core.model.LibraryNotice
import com.arthurnagy.dexlens.core.model.OpenSourceLibrary
import com.arthurnagy.dexlens.core.testing.TestDispatcherProvider
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getOrThrow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

// Robolectric: aboutlibraries-core's Android build logs through android.util.Log.
@RunWith(AndroidJUnit4::class)
internal class BundledOpenSourceLibraryRepositoryTest {
    private val source = FakeNoticesSource()

    private fun TestScope.repository() = BundledOpenSourceLibraryRepository(source, TestDispatcherProvider(testScheduler))

    private fun <T> Result<T, DomainError>.ok(): T = getOrThrow { error("unexpected $it") }

    // Scenario: Libraries listed with their licences
    @Test
    fun librariesAreListedInNameOrderWithVersionsAndLicenceNames() = runTest {
        assertEquals(
            listOf(
                OpenSourceLibrary("org.example:real-copyright", "alpha", "1.0", listOf("BSD 3-Clause License")),
                OpenSourceLibrary(
                    "androidx.camera:camera-core",
                    "Camera Core",
                    "1.6.2",
                    listOf("Apache License 2.0", "BSD 3-Clause \"New\" or \"Revised\" License"),
                ),
                OpenSourceLibrary("com.michael-bull.kotlin-result:kotlin-result", "kotlin-result", "2.3.1", listOf("ISC License")),
                OpenSourceLibrary("com.google.ai.edge.litert:litert", "LiteRT Android Implementation", "2.3.2", listOf("Apache License 2.0")),
                OpenSourceLibrary("com.google.mlkit:text-recognition", "text-recognition", "16.0.1", listOf("ML Kit Terms of Service")),
            ),
            repository().libraries().ok(),
        )
    }

    @Test
    fun theListIsParsedOncePerRepository() = runTest {
        val repository = repository()

        repository.libraries().ok()
        repository.library("androidx.camera:camera-core").ok()
        repository.libraries().ok()

        assertEquals(1, source.listReads)
    }

    // Scenario: Licence text shown
    @Test
    fun aLibraryCarriesTheFullTextOfEachLicence() = runTest {
        val detail = repository().library("com.google.mlkit:text-recognition").ok()

        assertEquals("text-recognition", detail.library.name)
        assertEquals("16.0.1", detail.library.version)
        assertEquals(
            listOf(LibraryLicense("ML Kit Terms of Service", "ML Kit Terms of Service\n\nBy using ML Kit you agree to these terms.")),
            detail.licenses,
        )
    }

    // Scenario: Copyright holder shown
    @Test
    fun theHolderIsTheOrganisationElseTheDevelopers() = runTest {
        val repository = repository()

        assertEquals("The Android Open Source Project", repository.library("androidx.camera:camera-core").ok().copyrightHolder)
        assertEquals("The MediaPipe Authors, Google", repository.library("com.google.ai.edge.litert:litert").ok().copyrightHolder)
        assertEquals(null, repository.library("com.google.mlkit:text-recognition").ok().copyrightHolder)
    }

    // Scenario: Copyright holder shown
    @Test
    fun templateCopyrightLinesAreRemovedAndRealOnesKept() = runTest {
        val repository = repository()

        val bsd = repository.library("androidx.camera:camera-core").ok().licenses.last()
        assertEquals("Redistribution and use in source and binary forms are permitted.", bsd.text)
        val isc = repository.library("com.michael-bull.kotlin-result:kotlin-result").ok().licenses.single()
        assertEquals("ISC License\n\n\nPermission to use, copy, modify, and/or distribute this software is granted.", isc.text)
        val real = repository.library("org.example:real-copyright").ok().licenses.single()
        assertTrue(real.text.startsWith("Copyright (c) 2020 Alpha Authors."))
        listOf(bsd, isc, real).forEach { licence -> assertFalse("<owner>" in licence.text) }
    }

    // Scenario: Bundled notices shown
    @Test
    fun embeddedNoticesAreAttachedOnceWithoutTheOffsetsIndex() = runTest {
        val repository = repository()

        assertEquals(
            listOf(LibraryNotice("THIRD_PARTY_NOTICE.txt", "abseil-cpp\n\nApache License 2.0")),
            repository.library("com.google.ai.edge.litert:litert").ok().notices,
        )
        assertEquals(
            listOf(LibraryNotice("third_party_licenses.txt", "protobuf\n\nCopyright 2008 Google Inc.")),
            repository.library("com.google.mlkit:text-recognition").ok().notices,
        )
        assertEquals(emptyList(), repository.library("androidx.camera:camera-core").ok().notices)
        assertFalse(MLKIT_OFFSETS in source.assetReads)
    }

    // Scenario: Unknown library
    @Test
    fun anUnknownLibraryIsNotFound() = runTest {
        assertEquals(Err(DomainError.NotFound), repository().library("org.example:removed"))
    }

    // Scenario: Licences listed offline
    @Test
    fun theListComesFromTheBundledSourceAndItsFailureIsUnknown() = runTest {
        assertEquals(5, repository().libraries().ok().size)
        assertEquals(1, source.listReads)

        source.failure = IOException("missing resource")
        assertIs<DomainError.Unknown>(repository().libraries().component2())
    }

    // Scenario: Licence text offline
    @Test
    fun aLibraryComesFromTheBundledSourceAndANoticeFailureIsUnknown() = runTest {
        val repository = repository()
        repository.library("com.google.ai.edge.litert:litert").ok()
        assertEquals(listOf("embedded_notices/index.json", LITERT_NOTICE), source.assetReads)

        source.failure = IOException("missing asset")
        assertIs<DomainError.Unknown>(repository.library("com.google.ai.edge.litert:litert").component2())
    }

    @Test
    fun aBrokenListIsUnknownAndRetriedOnTheNextCall() = runTest {
        val broken = FakeNoticesSource(librariesJson = "not json")
        val repository = BundledOpenSourceLibraryRepository(broken, TestDispatcherProvider(testScheduler))

        assertIs<DomainError.Unknown>(repository.libraries().component2())
        assertIs<DomainError.Unknown>(repository.libraries().component2())
        assertEquals(2, broken.listReads)
    }
}
