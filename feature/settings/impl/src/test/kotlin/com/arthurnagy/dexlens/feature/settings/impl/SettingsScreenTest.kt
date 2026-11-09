package com.arthurnagy.dexlens.feature.settings.impl

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arthurnagy.dexlens.core.designsystem.AppTheme
import com.arthurnagy.dexlens.core.model.ContentPackState
import com.arthurnagy.dexlens.core.model.ThemeMode
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
internal class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(
        state: SettingsState = SettingsState.Initial,
        onTheme: (ThemeMode) -> Unit = {},
        onDownload: () -> Unit = {},
        onCancel: () -> Unit = {},
        onDelete: () -> Unit = {},
    ) {
        composeRule.setContent { AppTheme { SettingsContent(state, SnackbarHostState(), {}, onTheme, onDownload, onCancel, onDelete) } }
    }

    // Scenario: Download from settings
    @Test
    fun offersTheDownloadWithItsSize() {
        var started = false
        show(onDownload = { started = true })
        composeRule.onNodeWithText("About 140 MB to download, 25 MB on this device").assertExists()
        composeRule.onNodeWithTag("settings:download").performClick()
        assertTrue(started)
    }

    // Scenario: Progress shown
    @Test
    fun showsProgressAndCancels() {
        var cancelled = false
        show(state = SettingsState.Initial.copy(content = ContentPackState.Downloading(50, 2051)), onCancel = { cancelled = true })
        composeRule.onNodeWithText("50 of 2051").assertExists()
        composeRule.onNodeWithTag("settings:cancel").performClick()
        assertTrue(cancelled)
    }

    @Test
    fun interruptedOffersResumeAndDelete() {
        var resumed = false
        var deleted = false
        show(state = SettingsState.Initial.copy(content = ContentPackState.Interrupted(5, 10)), onDownload = {
            resumed = true
        }, onDelete = {
            deleted =
                true
        })
        composeRule.onNodeWithTag("settings:resume").performClick()
        composeRule.onNodeWithTag("settings:delete").performClick()
        assertTrue(resumed)
        assertTrue(deleted)
    }

    // Scenario: Delete frees content
    @Test
    fun readyOffersDelete() {
        var deleted = false
        show(state = SettingsState.Initial.copy(content = ContentPackState.Ready), onDelete = { deleted = true })
        composeRule.onNodeWithText("Downloaded and available offline.").assertExists()
        composeRule.onNodeWithTag("settings:delete").performClick()
        assertTrue(deleted)
    }

    // Scenario: Theme choice applies
    @Test
    fun selectingAThemeCallsBack() {
        var selected: ThemeMode? = null
        show(onTheme = { selected = it })
        composeRule.onNodeWithTag("settings:theme:DARK").performClick()
        assertEquals(ThemeMode.DARK, selected)
    }

    // Scenario: Credits shown
    @Test
    fun creditsAndLegalAreShown() {
        show()
        composeRule.onNodeWithTag("settings:list").performScrollToNode(hasText("Pokémon data comes from PokeAPI", substring = true))
        composeRule.onNodeWithText("Pokémon names are trademarks", substring = true).assertExists()
    }

    // Scenario: Offer on first launch
    @Test
    fun offerShowsSizeAndChoices() {
        var skipped = false
        composeRule.setContent { AppTheme { ContentOfferContent(ContentPackState.NotDownloaded, {}, { skipped = true }, {}) } }
        composeRule.onNodeWithText("About 140 MB to download, 25 MB on this device").assertExists()
        composeRule.onNodeWithTag("offer:skip").performClick()
        assertTrue(skipped)
    }

    // Scenario: Progress shown
    @Test
    fun offerShowsProgressAndContinuesInBackground() {
        var hidden = false
        composeRule.setContent { AppTheme { ContentOfferContent(ContentPackState.Downloading(1, 4), {}, {}, { hidden = true }) } }
        composeRule.onNodeWithText("1 of 4").assertExists()
        composeRule.onNodeWithTag("offer:hide").performClick()
        assertTrue(hidden)
    }

    @Test
    fun offerResumesAndCompletes() {
        var resumed = false
        composeRule.setContent { AppTheme { ContentOfferContent(ContentPackState.Interrupted(1, 4), { resumed = true }, {}, {}) } }
        composeRule.onNodeWithTag("offer:resume").performClick()
        assertTrue(resumed)
    }

    @Test
    fun offerClosesWhenReady() {
        var closed = false
        composeRule.setContent { AppTheme { ContentOfferContent(ContentPackState.Ready, {}, {}, { closed = true }) } }
        composeRule.onNodeWithTag("offer:done").performClick()
        assertTrue(closed)
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun phone() {
        show()
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w841dp-h701dp-xxhdpi")
    fun foldable() {
        show()
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
    fun tablet() {
        show()
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun phoneDownloading() {
        show(state = SettingsState.Initial.copy(content = ContentPackState.Downloading(320, 2051)))
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel7)
    fun offer() {
        composeRule.setContent { AppTheme { ContentOfferContent(ContentPackState.NotDownloaded, {}, {}, {}) } }
        composeRule.onRoot().captureRoboImage()
    }
}
