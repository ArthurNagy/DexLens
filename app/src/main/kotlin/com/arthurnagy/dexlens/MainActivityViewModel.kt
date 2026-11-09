package com.arthurnagy.dexlens

import androidx.lifecycle.ViewModel
import com.arthurnagy.dexlens.core.domain.ObserveThemeModeUseCase
import com.arthurnagy.dexlens.core.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import javax.inject.Inject

/** App-level state the activity needs before any screen: the theme to render in. */
@HiltViewModel
internal class MainActivityViewModel
    @Inject
    constructor(
        private val observeThemeMode: ObserveThemeModeUseCase,
    ) : ViewModel(),
        OrbitContainerHost<MainActivityState, MainActivityState, Nothing> {
        override val container =
            orbitContainer<MainActivityState, Nothing>(MainActivityState.Initial) {
                observeThemeMode().collect { mode -> reduce { state.copy(loading = false, themeMode = mode) } }
            }
    }

internal data class MainActivityState(
    /** True until the stored theme is read — MainActivity draws nothing before (no wrong-theme flash). */
    val loading: Boolean,
    val themeMode: ThemeMode,
) {
    companion object {
        val Initial = MainActivityState(loading = true, themeMode = ThemeMode.SYSTEM)
    }
}
