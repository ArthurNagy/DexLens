package com.arthurnagy.dexlens.feature.settings.impl

import androidx.lifecycle.ViewModel
import com.arthurnagy.dexlens.core.domain.ObserveThemeModeUseCase
import com.arthurnagy.dexlens.core.domain.SetThemeModeUseCase
import com.arthurnagy.dexlens.core.domain.content.CancelContentDownloadUseCase
import com.arthurnagy.dexlens.core.domain.content.DeleteContentPackUseCase
import com.arthurnagy.dexlens.core.domain.content.ObserveContentPackStateUseCase
import com.arthurnagy.dexlens.core.domain.content.StartContentDownloadUseCase
import com.arthurnagy.dexlens.core.model.ThemeMode
import com.github.michaelbull.result.onErr
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
        private val observeContent: ObserveContentPackStateUseCase,
        private val startDownload: StartContentDownloadUseCase,
        private val cancelDownload: CancelContentDownloadUseCase,
        private val deleteContent: DeleteContentPackUseCase,
    ) : ViewModel(),
        OrbitContainerHost<SettingsState, SettingsState, Nothing> {
        override val container = orbitContainer<SettingsState, Nothing>(SettingsState.Initial) {
            observeTheme()
            observeContentState()
        }

        private fun observeTheme() = intent {
            observeThemeMode().collect { mode -> reduce { state.copy(themeMode = mode) } }
        }

        private fun observeContentState() = intent {
            observeContent().collect { content -> reduce { state.copy(content = content) } }
        }

        fun onThemeSelected(mode: ThemeMode) = intent {
            setThemeMode(mode).onErr { error -> reduce { state.copy(pendingError = error) } }
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
    }
