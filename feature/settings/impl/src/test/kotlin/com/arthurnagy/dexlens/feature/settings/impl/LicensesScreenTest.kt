package com.arthurnagy.dexlens.feature.settings.impl

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arthurnagy.dexlens.core.designsystem.AppTheme
import com.arthurnagy.dexlens.core.model.LibraryNotice
import com.arthurnagy.dexlens.core.testing.CoilLibrary
import com.arthurnagy.dexlens.core.testing.LiteRtLibrary
import com.arthurnagy.dexlens.core.testing.MitLibrary
import com.arthurnagy.dexlens.core.testing.SampleLibraryDetails
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
internal class LicensesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val listState = LicensesState(libraries = SampleLibraryDetails.map { it.library }, isLoading = false, pendingError = null)

    /** LiteRT with a notice long enough to scroll. */
    private val longNoticeState = LiteRtLibrary
        .copy(
            notices = listOf(
                LibraryNotice(
                    fileName = "THIRD_PARTY_NOTICE.txt",
                    text = (1..60).joinToString("\n\n") { n -> "Component $n\n\nLicensed under the Apache License, Version 2.0. See the licence text above for the terms." },
                ),
            ),
        ).let { detail ->
            LicenseDetailState(detail.library.name, detail.library.version, detail.toRows(), isLoading = false, notFound = false, pendingError = null)
        }

    private val notFoundState = LicenseDetailState.Initial.copy(isLoading = false, notFound = true)

    private fun showList(
        state: LicensesState = listState,
        onBack: (() -> Unit)? = {},
        onOpen: (String) -> Unit = {},
    ) {
        composeRule.setContent { AppTheme { LicensesContent(state, SnackbarHostState(), onBack, onOpen) } }
    }

    private fun showDetail(state: LicenseDetailState) {
        composeRule.setContent { AppTheme { LicenseDetailContent(state, SnackbarHostState(), onNavigateBack = {}) } }
    }

    // Scenario: Libraries listed with their licences
    @Test
    fun rowsShowNameVersionAndLicenceNames() {
        showList()

        composeRule.onNodeWithText("coil").assertIsDisplayed()
        composeRule.onNodeWithText("3.3.0 · Apache License 2.0").assertIsDisplayed()
        // No version: only the licence names, joined.
        composeRule.onNodeWithText("Example without version").assertIsDisplayed()
        composeRule.onNodeWithText("MIT License, Apache License 2.0").assertIsDisplayed()
    }

    // Scenario: Licence text shown
    @Test
    fun tappingALibraryReportsItsId() {
        var opened: String? = null
        showList(onOpen = { opened = it })

        composeRule.onNodeWithTag("licenses:library:${CoilLibrary.library.id}").performClick()

        assertEquals(CoilLibrary.library.id, opened)
    }

    @Test
    fun backShowsOnlyWhenGiven() {
        var back = false
        showList(onBack = { back = true })
        composeRule.onNodeWithTag("licenses:back").performClick()
        assertTrue(back)
    }

    @Test
    fun theRootListShowsNoBack() {
        showList(onBack = null)
        composeRule.onNodeWithTag("licenses:back").assertDoesNotExist()
    }

    @Test
    fun loadingShowsAProgressIndicator() {
        showList(state = LicensesState.Initial)
        composeRule.onNodeWithTag("licenses:loading").assertExists()
    }

    // Scenario: Licence placeholder on a wide window
    @Test
    fun placeholderAsksToPickALibrary() {
        composeRule.setContent { AppTheme { LicencePlaceholder() } }
        composeRule.onNodeWithText("Pick a library to see its licence.").assertIsDisplayed()
    }

    // Scenario: Copyright holder shown
    @Test
    fun detailShowsHolderLicencesAndNoTemplateLine() {
        val detail = LiteRtLibrary
        showDetail(LicenseDetailState(detail.library.name, detail.library.version, detail.toRows(), isLoading = false, notFound = false, pendingError = null))

        composeRule.onNodeWithText("LiteRT").assertIsDisplayed()
        composeRule.onNodeWithText("Version 1.4.0").assertIsDisplayed()
        composeRule.onNodeWithText("Copyright: Google LLC").assertIsDisplayed()
        composeRule.onNodeWithText("Apache License 2.0").assertIsDisplayed()
        composeRule.onNodeWithText("<owner>", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("<year>", substring = true).assertDoesNotExist()
    }

    // Scenario: Bundled notices shown
    @Test
    fun detailShowsTheNoticeAfterTheLicences() {
        showDetail(longNoticeState)

        composeRule.onNodeWithTag("licence:rows").performScrollToNode(hasText("THIRD_PARTY_NOTICE.txt"))
        composeRule.onNodeWithText("THIRD_PARTY_NOTICE.txt").assertIsDisplayed()
        composeRule.onNodeWithTag("licence:rows").performScrollToNode(hasText("Component 60"))
        composeRule.onNodeWithText("Component 60").assertIsDisplayed()
    }

    // Scenario: Licence text shown
    @Test
    fun aLibraryWithoutVersionOrHolderShowsItsLicences() {
        val detail = MitLibrary
        showDetail(LicenseDetailState(detail.library.name, detail.library.version, detail.toRows(), isLoading = false, notFound = false, pendingError = null))

        composeRule.onNodeWithText("Example without version").assertIsDisplayed()
        composeRule.onNodeWithTag("licence:version").assertDoesNotExist()
        composeRule.onNodeWithText("Copyright:", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("MIT License").assertIsDisplayed()
        composeRule.onNodeWithText("THE SOFTWARE IS PROVIDED", substring = true).assertIsDisplayed()
    }

    // Scenario: Unknown library
    @Test
    fun notFoundShowsAMessageInsteadOfRows() {
        showDetail(notFoundState)

        composeRule.onNodeWithText("This library can't be found. An update may have removed it.").assertIsDisplayed()
        composeRule.onNodeWithTag("licence:rows").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun phoneList() {
        showList()
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
    fun tabletList() {
        showList()
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun phoneDetail() {
        showDetail(longNoticeState)
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
    fun tabletDetail() {
        showDetail(longNoticeState)
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun phoneNotFound() {
        showDetail(notFoundState)
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
    fun tabletNotFound() {
        showDetail(notFoundState)
        composeRule.onRoot().captureRoboImage()
    }
}
