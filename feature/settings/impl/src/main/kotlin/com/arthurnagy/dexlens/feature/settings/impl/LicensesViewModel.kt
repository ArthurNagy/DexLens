package com.arthurnagy.dexlens.feature.settings.impl

import androidx.lifecycle.ViewModel
import com.arthurnagy.dexlens.core.domain.about.GetOpenSourceLibrariesUseCase
import com.github.michaelbull.result.onErr
import com.github.michaelbull.result.onOk
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import javax.inject.Inject

/** Loads the libraries bundled in the app, in name order. */
@HiltViewModel
internal class LicensesViewModel
    @Inject
    constructor(
        private val getLibraries: GetOpenSourceLibrariesUseCase,
    ) : ViewModel(),
        OrbitContainerHost<LicensesState, LicensesState, Nothing> {
        override val container = orbitContainer<LicensesState, Nothing>(LicensesState.Initial) {
            load()
        }

        private fun load() = intent {
            getLibraries()
                .onOk { libraries -> reduce { state.copy(libraries = libraries, isLoading = false) } }
                .onErr { error -> reduce { state.copy(isLoading = false, pendingError = error) } }
        }

        fun onErrorConsumed() = intent {
            reduce { state.copy(pendingError = null) }
        }
    }
