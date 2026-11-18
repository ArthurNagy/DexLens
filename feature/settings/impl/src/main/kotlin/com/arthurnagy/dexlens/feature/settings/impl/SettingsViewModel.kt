package com.arthurnagy.dexlens.feature.settings.impl

import androidx.lifecycle.ViewModel
import com.arthurnagy.dexlens.core.domain.ObserveScannerEnabledUseCase
import com.arthurnagy.dexlens.core.domain.ObserveThemeModeUseCase
import com.arthurnagy.dexlens.core.domain.SetScannerEnabledUseCase
import com.arthurnagy.dexlens.core.domain.SetThemeModeUseCase
import com.arthurnagy.dexlens.core.domain.about.GetAppVersionUseCase
import com.arthurnagy.dexlens.core.domain.connectivity.ObserveInternetAvailableUseCase
import com.arthurnagy.dexlens.core.domain.content.CancelContentDownloadUseCase
import com.arthurnagy.dexlens.core.domain.content.DeleteContentPackUseCase
import com.arthurnagy.dexlens.core.domain.content.ObserveContentPackStateUseCase
import com.arthurnagy.dexlens.core.domain.content.StartContentDownloadUseCase
import com.arthurnagy.dexlens.core.model.ThemeMode
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import javax.inject.Inject

@HiltViewModel
internal class SettingsViewModel
    @Inject
    constructor(
        private val observeThemeMode: ObserveThemeModeUseCase,
        private val setThemeMode: SetThemeModeUseCase,
        private val observeScannerEnabled: ObserveScannerEnabledUseCase,
        private val setScannerEnabled: SetScannerEnabledUseCase,
        private val observeContent: ObserveContentPackStateUseCase,
        private val startDownload: StartContentDownloadUseCase,
        private val cancelDownload: CancelContentDownloadUseCase,
        private val deleteContent: DeleteContentPackUseCase,
        private val getAppVersion: GetAppVersionUseCase,
        private val observeInternetAvailable: ObserveInternetAvailableUseCase,
    ) : ViewModel(),
        OrbitContainerHost<SettingsState, SettingsState, Nothing> {
        override val container = orbitContainer<SettingsState, Nothing>(SettingsState.Initial) {
            observeTheme()
            observeScannerSwitch()
            observeContentState()
            observeConnectivity()
            loadVersion()
        }

        /** A version that can't be read leaves [SettingsState.versionName] null, and the screen shows no version line. */
        private fun loadVersion() = intent {
            getAppVersion().onOk { version -> reduce { state.copy(versionName = version) } }
        }

        private fun observeTheme() = intent {
            observeThemeMode().collect { mode -> reduce { state.copy(themeMode = mode) } }
        }

        private fun observeScannerSwitch() = intent {
            observeScannerEnabled().collect { enabled -> reduce { state.copy(scannerEnabled = enabled) } }
        }

        private fun observeContentState() = intent {
            observeContent().collect { content -> reduce { state.copy(content = content) } }
        }

        /** Only while settings is on screen, so the system network callback isn't held in the background. */
        private fun observeConnectivity() = intent {
            repeatOnSubscription {
                observeInternetAvailable().collect { available -> reduce { state.copy(internetAvailable = available) } }
            }
        }

        fun onThemeSelected(mode: ThemeMode) = intent {
            setThemeMode(mode).onErr { error -> reduce { state.copy(pendingError = error) } }
        }

        fun onScannerEnabledChanged(enabled: Boolean) = intent {
            setScannerEnabled(enabled).onErr { error -> reduce { state.copy(pendingError = error) } }
        }

        fun onDownload() = intent {
            startDownload()
        }

        fun onCancelDownload() = intent {
            cancelDownload()
        }

        fun onDeleteContent() = intent {
            deleteContent().onErr { error -> reduce { state.copy(pendingError = error) } }
        }

        fun onErrorConsumed() = intent {
            reduce { state.copy(pendingError = null) }
        }

        /** No app on the device could open the source code link. */
        fun onSourceLinkUnavailable() = intent {
            reduce { state.copy(pendingMessage = R.string.settings_source_unavailable) }
        }

        /** Neither the internet panel nor network settings could be opened on this device. */
        fun onNetworkSettingsUnavailable() = intent {
            reduce { state.copy(pendingMessage = R.string.settings_network_settings_unavailable) }
        }

        fun onMessageConsumed() = intent {
            reduce { state.copy(pendingMessage = null) }
        }
    }
