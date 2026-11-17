package com.arthurnagy.dexlens.feature.settings.impl

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.about.GetOpenSourceLibraryUseCase
import com.arthurnagy.dexlens.core.domain.about.GetOpenSourceLibrariesUseCase
import com.arthurnagy.dexlens.core.testing.FakeOpenSourceLibraryRepository
import com.arthurnagy.dexlens.core.testing.KotlinResultLibrary
import com.arthurnagy.dexlens.core.testing.LiteRtLibrary
import com.arthurnagy.dexlens.core.testing.MitLibrary
import com.arthurnagy.dexlens.core.testing.SampleLibraryDetails
import com.arthurnagy.dexlens.core.testing.TestDispatcherProvider
import com.arthurnagy.dexlens.feature.settings.api.LicenseDetailKey
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import kotlin.test.Test
import kotlin.test.assertEquals

class LicenceViewModelsTest {
    private val repository = FakeOpenSourceLibraryRepository()

    private val apacheRows = listOf(
        LicenceRow.SectionTitle("Apache License 2.0"),
        LicenceRow.Paragraph("Licensed under the Apache License, Version 2.0 (the \"License\");\nyou may not use this file except in compliance with the License."),
        LicenceRow.Paragraph("Unless required by applicable law or agreed to in writing, software\ndistributed under the License is distributed on an \"AS IS\" BASIS."),
    )

    private fun list() = LicensesViewModel(GetOpenSourceLibrariesUseCase(repository))

    private fun TestScope.detail(id: String) =
        LicenseDetailViewModel(LicenseDetailKey(id), GetOpenSourceLibraryUseCase(repository), TestDispatcherProvider(testScheduler))

    // Scenario: Libraries listed with their licences
    @Test
    fun theListShowsEveryLibraryInTheRepositorysOrder() = runTest {
        list().testWithInternalState(this, LicensesState.Initial) {
            runOnCreate()
            expectInternalState { copy(libraries = SampleLibraryDetails.map { it.library }, isLoading = false) }
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun aListFailureIsReportedOnce() = runTest {
        repository.error = DomainError.Unknown("broken")
        list().testWithInternalState(this, LicensesState.Initial) {
            runOnCreate()
            expectInternalState { copy(isLoading = false, pendingError = DomainError.Unknown("broken")) }
            containerHost.onErrorConsumed()
            expectInternalState { copy(pendingError = null) }
            cancelAndIgnoreRemainingItems()
        }
    }

    // Scenario: Licence text shown
    @Test
    fun aLibraryShowsItsNameVersionHolderAndLicenceParagraphs() = runTest {
        detail(MitLibrary.library.id).testWithInternalState(this, LicenseDetailState.Initial) {
            runOnCreate()
            expectInternalState {
                copy(
                    name = "Example without version",
                    version = null,
                    rows = listOf(
                        LicenceRow.SectionTitle("MIT License"),
                        LicenceRow.Paragraph("Permission is hereby granted, free of charge, to any person obtaining a copy of this software."),
                        LicenceRow.Paragraph("THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND."),
                    ) + apacheRows,
                    isLoading = false,
                )
            }
            cancelAndIgnoreRemainingItems()
        }
    }

    // Scenario: Licence text shown
    @Test
    fun theHolderLeadsTheRows() = runTest {
        detail(KotlinResultLibrary.library.id).testWithInternalState(this, LicenseDetailState.Initial) {
            runOnCreate()
            expectInternalState {
                copy(
                    name = "kotlin-result",
                    version = "2.1.0",
                    rows = listOf(
                        LicenceRow.Holder("Michael Bull"),
                        LicenceRow.SectionTitle("ISC License"),
                        LicenceRow.Paragraph("Permission to use, copy, modify, and/or distribute this software for any purpose is hereby granted."),
                    ),
                    isLoading = false,
                )
            }
            cancelAndIgnoreRemainingItems()
        }
    }

    // Scenario: Bundled notices shown
    @Test
    fun noticesFollowTheLicences() = runTest {
        detail(LiteRtLibrary.library.id).testWithInternalState(this, LicenseDetailState.Initial) {
            runOnCreate()
            expectInternalState {
                copy(
                    name = "LiteRT",
                    version = "1.4.0",
                    rows = listOf(LicenceRow.Holder("Google LLC")) + apacheRows + listOf(
                        LicenceRow.SectionTitle("THIRD_PARTY_NOTICE.txt"),
                        LicenceRow.Paragraph("abseil-cpp"),
                        LicenceRow.Paragraph("Apache License, Version 2.0"),
                        LicenceRow.Paragraph("flatbuffers"),
                        LicenceRow.Paragraph("Apache License, Version 2.0"),
                    ),
                    isLoading = false,
                )
            }
            cancelAndIgnoreRemainingItems()
        }
    }

    // Scenario: Unknown library
    @Test
    fun anUnknownLibraryIsNotFound() = runTest {
        detail("org.example:removed").testWithInternalState(this, LicenseDetailState.Initial) {
            runOnCreate()
            expectInternalState { copy(isLoading = false, notFound = true) }
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun anotherDetailFailureIsReportedOnce() = runTest {
        repository.error = DomainError.Unknown("broken")
        detail(LiteRtLibrary.library.id).testWithInternalState(this, LicenseDetailState.Initial) {
            runOnCreate()
            expectInternalState { copy(isLoading = false, pendingError = DomainError.Unknown("broken")) }
            containerHost.onErrorConsumed()
            expectInternalState { copy(pendingError = null) }
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun paragraphsSplitOnBlankLinesWithSpacesOrCarriageReturns() {
        val detail = MitLibrary.copy(
            copyrightHolder = null,
            licenses = listOf(MitLibrary.licenses.first().copy(text = "one\r\n\r\ntwo\n   \nthree\nstill three\n\n\n")),
        )
        assertEquals(
            listOf(LicenceRow.SectionTitle("MIT License"), LicenceRow.Paragraph("one"), LicenceRow.Paragraph("two"), LicenceRow.Paragraph("three\nstill three")),
            detail.toRows(),
        )
    }
}
