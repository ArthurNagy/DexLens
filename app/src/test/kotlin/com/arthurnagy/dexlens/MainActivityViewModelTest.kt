package com.arthurnagy.dexlens

import com.arthurnagy.dexlens.core.domain.ObserveThemeModeUseCase
import com.arthurnagy.dexlens.core.model.ThemeMode
import com.arthurnagy.dexlens.core.testing.FakeUserPreferencesRepository
import kotlinx.coroutines.test.runTest
import org.orbitmvi.orbit.test.testWithInternalState
import kotlin.test.Test

class MainActivityViewModelTest {
    @Test
    fun loadsThenFollowsTheStoredThemeMode() = runTest {
        val repository = FakeUserPreferencesRepository(ThemeMode.DARK)
        val viewModel = MainActivityViewModel(ObserveThemeModeUseCase(repository))

        viewModel.testWithInternalState(this, MainActivityState.Initial) {
            runOnCreate()
            expectInternalState { copy(loading = false, themeMode = ThemeMode.DARK) }
            repository.setThemeMode(ThemeMode.LIGHT)
            expectInternalState { copy(themeMode = ThemeMode.LIGHT) }
            cancelAndIgnoreRemainingItems()
        }
    }
}
