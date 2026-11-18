package com.arthurnagy.dexlens.feature.settings.impl

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.ObserveScannerEnabledUseCase
import com.arthurnagy.dexlens.core.domain.ObserveThemeModeUseCase
import com.arthurnagy.dexlens.core.domain.SetScannerEnabledUseCase
import com.arthurnagy.dexlens.core.domain.SetThemeModeUseCase
import com.arthurnagy.dexlens.core.domain.about.GetAppVersionUseCase
import com.arthurnagy.dexlens.core.domain.connectivity.ObserveInternetAvailableUseCase
import com.arthurnagy.dexlens.core.domain.content.CancelContentDownloadUseCase
import com.arthurnagy.dexlens.core.domain.content.DeleteContentPackUseCase
import com.arthurnagy.dexlens.core.domain.content.MarkContentOfferShownUseCase
import com.arthurnagy.dexlens.core.domain.content.ObserveContentPackStateUseCase
import com.arthurnagy.dexlens.core.domain.content.StartContentDownloadUseCase
import com.arthurnagy.dexlens.core.model.ContentPackState
import com.arthurnagy.dexlens.core.model.ThemeMode
import com.arthurnagy.dexlens.core.testing.FakeAppInfoRepository
import com.arthurnagy.dexlens.core.testing.FakeConnectivityRepository
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
    private val appInfo = FakeAppInfoRepository(version = "0.1.0")
    private val connectivity = FakeConnectivityRepository()

    /** The state once the version has loaded, so tests of other intents see only their own change. */
    private val loaded = SettingsState.Initial.copy(versionName = "0.1.0")

    private fun settings() = SettingsViewModel(
        ObserveThemeModeUseCase(preferences),
        SetThemeModeUseCase(preferences),
        ObserveScannerEnabledUseCase(preferences),
        SetScannerEnabledUseCase(preferences),
        ObserveContentPackStateUseCase(content),
        StartContentDownloadUseCase(content),
        CancelContentDownloadUseCase(content),
        DeleteContentPackUseCase(content),
        GetAppVersionUseCase(appInfo),
        ObserveInternetAvailableUseCase(connectivity),
    )

    private fun offer() = ContentOfferViewModel(
        ObserveContentPackStateUseCase(content),
        StartContentDownloadUseCase(content),
        MarkContentOfferShownUseCase(preferences),
        ObserveInternetAvailableUseCase(connectivity),
    )

    // Scenario: Theme choice applies
    @Test
    fun themeSelectionIsStored() = runTest {
        settings().testWithInternalState(this, loaded) {
            runOnCreate()
            containerHost.onThemeSelected(ThemeMode.DARK)
            expectInternalState { copy(themeMode = ThemeMode.DARK) }
            cancelAndIgnoreRemainingItems()
        }
    }

    // Scenario: Scanner switch applies
    @Test
    fun scannerSwitchIsStored() = runTest {
        settings().testWithInternalState(this, loaded) {
            runOnCreate()
            containerHost.onScannerEnabledChanged(false)
            expectInternalState { copy(scannerEnabled = false) }
            cancelAndIgnoreRemainingItems()
        }
        assertEquals(false, preferences.scannerEnabled.first())
    }

    @Test
    fun aFailedScannerSwitchWriteIsReported() = runTest {
        settings().testWithInternalState(this, loaded) {
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
        settings().testWithInternalState(this, loaded) {
            runOnCreate()
            content.state.value = ContentPackState.Downloading(3, 10)
            expectInternalState { copy(content = ContentPackState.Downloading(3, 10)) }
            cancelAndIgnoreRemainingItems()
        }
    }

    // Scenario: Download from settings
    @Test
    fun downloadActionStartsTheDownload() = runTest {
        settings().testWithInternalState(this, loaded) {
            runOnCreate()
            containerHost.onDownload().join()
            cancelAndIgnoreRemainingItems()
        }
        assertEquals(1, content.started)
    }

    @Test
    fun cancelAndDeleteCallThrough() = runTest {
        settings().testWithInternalState(this, loaded) {
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
            expectInternalState { ContentOfferState.Initial.copy(content = ContentPackState.Downloading(1, 2)) }
            cancelAndIgnoreRemainingItems()
        }
    }

    @Test
    fun aFailedThemeWriteIsReportedOnce() = runTest {
        preferences.failNextWrite = DomainError.Unknown("full")
        settings().testWithInternalState(this, loaded) {
            runOnCreate()
            containerHost.onThemeSelected(ThemeMode.DARK)
            expectInternalState { copy(pendingError = DomainError.Unknown("full")) }
            containerHost.onErrorConsumed()
            expectInternalState { copy(pendingError = null) }
            cancelAndIgnoreRemainingItems()
        }
    }

    // Scenario: Version shown
    @Test
    fun theInstalledVersionIsLoaded() = runTest {
        settings().testWithInternalState(this, SettingsState.Initial) {
            runOnCreate()
            expectInternalState { copy(versionName = "0.1.0") }
            cancelAndIgnoreRemainingItems()
        }
    }

    // Scenario: Version unavailable
    @Test
    fun anUnreadableVersionLeavesItUnknown() = runTest {
        appInfo.error = DomainError.Unknown("no version")
        settings().testWithInternalState(this, SettingsState.Initial) {
            runOnCreate()
            containerHost.onSourceLinkUnavailable()
            // The version failure changes nothing and reports nothing: the next state is the link message's.
            expectInternalState { copy(pendingMessage = R.string.settings_source_unavailable) }
            cancelAndIgnoreRemainingItems()
        }
    }

    // Scenario: No app for the source link
    @Test
    fun anUnopenableSourceLinkShowsAMessageOnce() = runTest {
        settings().testWithInternalState(this, loaded) {
            runOnCreate()
            containerHost.onSourceLinkUnavailable()
            expectInternalState { copy(pendingMessage = R.string.settings_source_unavailable) }
            containerHost.onMessageConsumed()
            expectInternalState { copy(pendingMessage = null) }
            cancelAndIgnoreRemainingItems()
        }
    }

    // Scenario: Offer while offline
    // Scenario: Offer shows Download once connected
    // Scenario: Offer goes back offline
    @Test
    fun offerFollowsTheConnection() = runTest {
        connectivity.internetAvailable.value = false
        offer().testWithInternalState(this, ContentOfferState.Initial) {
            runOnCreate()
            expectInternalState { copy(internetAvailable = false) }
            connectivity.internetAvailable.value = true
            expectInternalState { copy(internetAvailable = true) }
            connectivity.internetAvailable.value = false
            expectInternalState { copy(internetAvailable = false) }
            cancelAndIgnoreRemainingItems()
        }
    }

    // Scenario: Skip while offline
    @Test
    fun anOfflineOfferIsStillAnsweredAndStartsNothing() = runTest {
        connectivity.internetAvailable.value = false
        offer().testWithInternalState(this, ContentOfferState.Initial) {
            runOnCreate()
            expectInternalState { copy(internetAvailable = false) }
            cancelAndIgnoreRemainingItems()
        }
        assertEquals(true, preferences.contentOfferShown.first())
        assertEquals(0, content.started)
    }

    // Scenario: Offer can't open network settings
    @Test
    fun offerSaysNetworkSettingsCantOpenUntilTheNextTryOrAConnection() = runTest {
        connectivity.internetAvailable.value = false
        offer().testWithInternalState(this, ContentOfferState.Initial) {
            runOnCreate()
            expectInternalState { copy(internetAvailable = false) }
            containerHost.onNetworkSettingsUnavailable()
            expectInternalState { copy(networkSettingsUnavailable = true) }
            containerHost.onConnectTapped()
            expectInternalState { copy(networkSettingsUnavailable = false) }
            containerHost.onNetworkSettingsUnavailable()
            expectInternalState { copy(networkSettingsUnavailable = true) }
            connectivity.internetAvailable.value = true
            expectInternalState { copy(internetAvailable = true, networkSettingsUnavailable = false) }
            cancelAndIgnoreRemainingItems()
        }
    }

    // Scenario: Download from settings while offline
    // Scenario: Settings download returns when connected
    @Test
    fun settingsFollowsTheConnection() = runTest {
        connectivity.internetAvailable.value = false
        settings().testWithInternalState(this, loaded) {
            runOnCreate()
            expectInternalState { copy(internetAvailable = false) }
            connectivity.internetAvailable.value = true
            expectInternalState { copy(internetAvailable = true) }
            connectivity.internetAvailable.value = false
            expectInternalState { copy(internetAvailable = false) }
            cancelAndIgnoreRemainingItems()
        }
    }

    // Scenario: Connect from settings
    @Test
    fun unopenableNetworkSettingsShowAMessageOnce() = runTest {
        settings().testWithInternalState(this, loaded) {
            runOnCreate()
            containerHost.onNetworkSettingsUnavailable()
            expectInternalState { copy(pendingMessage = R.string.settings_network_settings_unavailable) }
            containerHost.onMessageConsumed()
            expectInternalState { copy(pendingMessage = null) }
            cancelAndIgnoreRemainingItems()
        }
    }
}
