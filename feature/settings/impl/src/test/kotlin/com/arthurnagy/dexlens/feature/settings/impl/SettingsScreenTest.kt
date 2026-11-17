package com.arthurnagy.dexlens.feature.settings.impl

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arthurnagy.dexlens.core.designsystem.AppTheme
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.ObserveScannerEnabledUseCase
import com.arthurnagy.dexlens.core.domain.ObserveThemeModeUseCase
import com.arthurnagy.dexlens.core.domain.SetScannerEnabledUseCase
import com.arthurnagy.dexlens.core.domain.SetThemeModeUseCase
import com.arthurnagy.dexlens.core.domain.about.GetAppVersionUseCase
import com.arthurnagy.dexlens.core.domain.content.CancelContentDownloadUseCase
import com.arthurnagy.dexlens.core.domain.content.DeleteContentPackUseCase
import com.arthurnagy.dexlens.core.domain.content.ObserveContentPackStateUseCase
import com.arthurnagy.dexlens.core.domain.content.StartContentDownloadUseCase
import com.arthurnagy.dexlens.core.model.ContentPackState
import com.arthurnagy.dexlens.core.model.ThemeMode
import com.arthurnagy.dexlens.core.testing.FakeAppInfoRepository
import com.arthurnagy.dexlens.core.testing.FakeContentPackRepository
import com.arthurnagy.dexlens.core.testing.FakeUserPreferencesRepository
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        onScanner: (Boolean) -> Unit = {},
        onLicences: () -> Unit = {},
        onSource: () -> Unit = {},
    ) {
        composeRule.setContent {
            AppTheme {
                SettingsContent(state, SnackbarHostState(), {}, onTheme, onDownload, onCancel, onDelete, onScanner, onLicences, onSource)
            }
        }
    }

    private fun scrollTo(tag: String) {
        composeRule.onNodeWithTag("settings:list").performScrollToNode(hasTestTag(tag))
    }

    private fun settingsViewModel(appInfo: FakeAppInfoRepository = FakeAppInfoRepository(version = "0.1.0")): SettingsViewModel {
        val content = FakeContentPackRepository()
        val preferences = FakeUserPreferencesRepository(ThemeMode.SYSTEM)
        return SettingsViewModel(
            ObserveThemeModeUseCase(preferences),
            SetThemeModeUseCase(preferences),
            ObserveScannerEnabledUseCase(preferences),
            SetScannerEnabledUseCase(preferences),
            ObserveContentPackStateUseCase(content),
            StartContentDownloadUseCase(content),
            CancelContentDownloadUseCase(content),
            DeleteContentPackUseCase(content),
            GetAppVersionUseCase(appInfo),
        )
    }

    // Scenario: Download from settings
    @Test
    fun offersTheDownloadWithItsSize() {
        var started = false
        show(onDownload = { started = true })
        composeRule.onNodeWithText("About 183 MB to download, 27 MB on this device").assertExists()
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
        composeRule.onNodeWithTag("settings:list").performScrollToNode(hasTestTag("settings:theme:DARK"))
        composeRule.onNodeWithTag("settings:theme:DARK").performClick()
        assertEquals(ThemeMode.DARK, selected)
    }

    // Scenario: Scanner switch applies
    @Test
    fun scannerSwitchShowsItsStateAndCallsBack() {
        var changed: Boolean? = null
        show(onScanner = { changed = it })
        composeRule.onNodeWithTag("settings:list").performScrollToNode(hasTestTag("settings:scanner"))
        composeRule.onNodeWithTag("settings:scanner").assertIsOn().performClick()
        assertEquals(false, changed)
    }

    @Test
    fun scannerSwitchShowsWhenOff() {
        show(state = SettingsState.Initial.copy(scannerEnabled = false))
        composeRule.onNodeWithTag("settings:list").performScrollToNode(hasTestTag("settings:scanner"))
        composeRule.onNodeWithTag("settings:scanner").assertIsOff()
    }

    // Scenario: Credits shown
    @Test
    fun creditsAndLegalAreShown() {
        show()
        listOf(
            "PokeAPI (pokeapi.co), used under the BSD-3-Clause licence",
            "ML Kit text recognition",
            "LiteRT",
            "Image recognition uses a MobileNetV4 model (timm, Apache-2.0), fine-tuned for DexLens.",
            "DexLens is open source under the MIT License.",
            "Unofficial fan project",
        ).forEach { text ->
            composeRule.onNodeWithTag("settings:list").performScrollToNode(hasText(text, substring = true))
            composeRule.onNodeWithText(text, substring = true).assertIsDisplayed()
        }
    }

    // Scenario: Version shown
    @Test
    fun theVersionIsShown() {
        show(state = SettingsState.Initial.copy(versionName = "0.1.0"))
        scrollTo("settings:version")
        composeRule.onNodeWithText("Version 0.1.0").assertIsDisplayed()
    }

    // Scenario: Version shown
    @Test
    fun theScreenLoadsTheInstalledVersion() {
        composeRule.setContent { AppTheme { SettingsScreen({}, {}, viewModel = settingsViewModel()) } }
        scrollTo("settings:version")
        composeRule.onNodeWithText("Version 0.1.0").assertIsDisplayed()
    }

    // Scenario: Version unavailable
    @Test
    fun anUnknownVersionShowsNoVersionLine() {
        val appInfo = FakeAppInfoRepository().apply { error = DomainError.Unknown("no version") }
        composeRule.setContent { AppTheme { SettingsScreen({}, {}, viewModel = settingsViewModel(appInfo)) } }
        scrollTo("settings:source")
        composeRule.onNodeWithTag("settings:version").assertDoesNotExist()
        composeRule.onNodeWithText("Version", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("DexLens is open source under the MIT License.").assertExists()
        composeRule.onNodeWithTag("settings:licenses").assertIsDisplayed()
    }

    // Scenario: Licences opened from settings
    @Test
    fun theLicencesRowOpensTheLicences() {
        var opened = false
        show(onLicences = { opened = true })
        scrollTo("settings:licenses")
        composeRule.onNodeWithTag("settings:licenses").assertTextContains("Open-source licences", substring = true).performClick()
        assertTrue(opened)
    }

    // Scenario: Source code opened in the browser
    @Test
    fun theSourceRowHandsTheLinkToAnotherApp() {
        var unavailable = false
        composeRule.setContent {
            AppTheme {
                val openSource = rememberOpenLink("https://github.com/ArthurNagy/DexLens") { unavailable = true }
                SettingsContent(SettingsState.Initial, SnackbarHostState(), {}, {}, {}, {}, {}, {}, {}, openSource)
            }
        }
        scrollTo("settings:source")
        composeRule.onNodeWithTag("settings:source").performClick()

        val started = shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, started.action)
        assertEquals(Uri.parse("https://github.com/ArthurNagy/DexLens"), started.data)
        assertFalse(unavailable)
    }

    // Scenario: No app for the source link
    @Test
    fun withoutAnAppForTheLinkAMessageShows() {
        // Robolectric then throws ActivityNotFoundException for an intent no installed activity handles.
        shadowOf(ApplicationProvider.getApplicationContext<Application>()).checkActivities(true)
        composeRule.setContent { AppTheme { SettingsScreen({}, {}, viewModel = settingsViewModel()) } }
        scrollTo("settings:source")
        composeRule.onNodeWithTag("settings:source").performClick()

        composeRule.waitUntil {
            composeRule.onAllNodesWithText("The link can't be opened", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("The link can't be opened. No app on this device opens web links.").assertIsDisplayed()
    }

    // Scenario: Offer on first launch
    @Test
    fun offerShowsSizeAndChoices() {
        var skipped = false
        composeRule.setContent { AppTheme { ContentOfferContent(ContentPackState.NotDownloaded, {}, { skipped = true }, {}) } }
        composeRule.onNodeWithText("About 183 MB to download, 27 MB on this device").assertExists()
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
    fun phoneAbout() {
        show(state = SettingsState.Initial.copy(versionName = "0.1.0"))
        scrollTo("settings:source")
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.MediumTablet)
    fun tabletAbout() {
        show(state = SettingsState.Initial.copy(versionName = "0.1.0"))
        scrollTo("settings:source")
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
