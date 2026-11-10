package com.arthurnagy.dexlens.feature.settings.impl

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.ObserveScannerEnabledUseCase
import com.arthurnagy.dexlens.core.domain.ObserveThemeModeUseCase
import com.arthurnagy.dexlens.core.domain.SetScannerEnabledUseCase
import com.arthurnagy.dexlens.core.domain.SetThemeModeUseCase
import com.arthurnagy.dexlens.core.domain.content.CancelContentDownloadUseCase
import com.arthurnagy.dexlens.core.domain.content.DeleteContentPackUseCase
import com.arthurnagy.dexlens.core.domain.content.MarkContentOfferShownUseCase
import com.arthurnagy.dexlens.core.domain.content.ObserveContentPackStateUseCase
import com.arthurnagy.dexlens.core.domain.content.StartContentDownloadUseCase
import com.arthurnagy.dexlens.core.model.ContentPackState
import com.arthurnagy.dexlens.core.model.ThemeMode
import com.arthurnagy.dexlens.core.testing.FakeContentPackRepository
import com.arthurnagy.dexlens.core.testing.FakeUserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsViewModelTest {
    private val content = FakeContentPackRepository()
    private val preferences = FakeUserPreferencesRepository(ThemeMode.SYSTEM)

    private fun settings() = SettingsViewModel(
        ObserveThemeModeUseCase(preferences),
        SetThemeModeUseCase(preferences),
        ObserveScannerEnabledUseCase(preferences),
        SetScannerEnabledUseCase(preferences),
        ObserveContentPackStateUseCase(content),
        StartContentDownloadUseCase(content),
        CancelContentDownloadUseCase(content),
        DeleteContentPackUseCase(content),
    )

    private fun offer() = ContentOfferViewModel(
        ObserveContentPackStateUseCase(content),
        StartContentDownloadUseCase(content),
        MarkContentOfferShownUseCase(preferences),
    )

    // Scenario: Theme choice applies
    @Test
    fun themeSelectionIsStored() = runTest {
        settings().testWithInternalState(this, SettingsState.Initial) {
            runOnCreate()
            containerHost.onThemeSelected(ThemeMode.DARK)
            expectInternalState { copy(themeMode = ThemeMode.DARK) }
            cancelAndIgnoreRemainingItems()
        }
    }

    // Scenario: Scanner switch applies
    @Test
    fun scannerSwitchIsStored() = runTest {
        settings().testWithInternalState(this, SettingsState.Initial) {
            runOnCreate()
            containerHost.onScannerEnabledChanged(false)
            expectInternalState { copy(scannerEnabled = false) }
            cancelAndIgnoreRemainingItems()
        }
        assertEquals(false, preferences.scannerEnabled.first())
    }

    @Test
    fun aFailedScannerSwitchWriteIsReported() = runTest {
        settings().testWithInternalState(this, SettingsState.Initial) {
            runOnCreate()
            preferences.failNextWrite = DomainError.Unknown("disk full")
            containerHost.onScannerEnabledChanged(false)
            expectInternalState { copy(pendingError = DomainError.Unknown("disk full")) }
            cancelAndIgnoreRemainingItems()
        }
    }

    // Scenario: Progress shown
    @Test
    fun contentStateIsObserved() = runTest {
        settings().testWithInternalState(this, SettingsState.Initial) {
            runOnCreate()
            content.state.value = ContentPackState.Downloading(3, 10)
            expectInternalState { copy(content = ContentPackState.Downloading(3, 10)) }
            cancelAndIgnoreRemainingItems()
        }
    }

    // Scenario: Download from settings
    @Test
    fun downloadActionStartsTheDownload() = runTest {
        settings().testWithInternalState(this, SettingsState.Initial) {
            runOnCreate()
            containerHost.onDownload().join()
            cancelAndIgnoreRemainingItems()
        }
        assertEquals(1, content.started)
    }

    @Test
    fun cancelAndDeleteCallThrough() = runTest {
        settings().testWithInternalState(this, SettingsState.Initial) {
            runOnCreate()
            containerHost.onCancelDownload().join()
            containerHost.onDeleteContent().join()
            cancelAndIgnoreRemainingItems()
        }
        assertEquals(1, content.cancelled)
        assertEquals(1, content.deleted)
    }

    // Scenario: Skip is remembered
    @Test
    fun seeingTheOfferRemembersIt() = runTest {
        offer().testWithInternalState(this, ContentOfferState.Initial) {
            containerHost.markAnswered().join()
            cancelAndIgnoreRemainingItems()
        }
        assertEquals(true, preferences.contentOfferShown.first())
        assertEquals(0, content.started)
    }

    @Test
    fun acceptingTheOfferStartsTheDownload() = runTest {
        offer().testWithInternalState(this, ContentOfferState.Initial) {
            runOnCreate()
            containerHost.onDownload().join()
            cancelAndIgnoreRemainingItems()
        }
        assertEquals(1, content.started)
    }

    @Test
    fun offerFollowsProgress() = runTest {
        offer().testWithInternalState(this, ContentOfferState.Initial) {
            runOnCreate()
            content.state.value = ContentPackState.Downloading(1, 2)
            expectInternalState { ContentOfferState(ContentPackState.Downloading(1, 2)) }
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun aFailedThemeWriteIsReportedOnce() = runTest {
        preferences.failNextWrite = DomainError.Unknown("full")
        settings().testWithInternalState(this, SettingsState.Initial) {
            runOnCreate()
            containerHost.onThemeSelected(ThemeMode.DARK)
            expectInternalState { copy(pendingError = DomainError.Unknown("full")) }
            containerHost.onErrorConsumed()
            expectInternalState { copy(pendingError = null) }
            cancelAndIgnoreRemainingItems()
        }
    }
}
