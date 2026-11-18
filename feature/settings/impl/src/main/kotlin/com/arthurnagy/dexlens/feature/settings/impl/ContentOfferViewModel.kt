package com.arthurnagy.dexlens.feature.settings.impl

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.arthurnagy.dexlens.core.domain.connectivity.ObserveConnectivityUseCase
import com.arthurnagy.dexlens.core.domain.content.MarkContentOfferShownUseCase
import com.arthurnagy.dexlens.core.domain.content.ObserveContentPackStateUseCase
import com.arthurnagy.dexlens.core.domain.content.StartContentDownloadUseCase
import com.arthurnagy.dexlens.core.model.Connectivity
import com.arthurnagy.dexlens.core.model.ContentPackState
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import javax.inject.Inject

@Immutable
internal data class ContentOfferState(
    val content: ContentPackState,
    /** Starts [Connectivity.Online], so the first frame looks as it did before connectivity was read: a tap then still works as today. */
    val connectivity: Connectivity,
    /** Network settings couldn't be opened: the offline offer says so under its Connect button until the next try. */
    val networkSettingsUnavailable: Boolean,
) {
    companion object {
        val Initial = ContentOfferState(
            content = ContentPackState.NotDownloaded,
            connectivity = Connectivity.Online,
            networkSettingsUnavailable = false,
        )
    }
}

/** The one-time offer. It counts as answered the moment it is seen, so dismissing it never shows it again. */
@HiltViewModel
internal class ContentOfferViewModel
    @Inject
    constructor(
        private val observeContent: ObserveContentPackStateUseCase,
        private val startDownload: StartContentDownloadUseCase,
        private val markOfferShown: MarkContentOfferShownUseCase,
        private val observeConnectivity: ObserveConnectivityUseCase,
    ) : ViewModel(),
        OrbitContainerHost<ContentOfferState, ContentOfferState, Nothing> {
        override val container = orbitContainer<ContentOfferState, Nothing>(ContentOfferState.Initial) {
            markAnswered()
            observeState()
            followConnectivity()
        }

        internal fun markAnswered() = intent {
            markOfferShown()
        }

        private fun observeState() = intent {
            observeContent().collect { content -> reduce { state.copy(content = content) } }
        }

        /** Only while the offer is on screen, so the system network callback isn't held in the background. */
        private fun followConnectivity() = intent {
            repeatOnSubscription {
                observeConnectivity().collect { connectivity ->
                    reduce {
                        state.copy(
                            connectivity = connectivity,
                            networkSettingsUnavailable = state.networkSettingsUnavailable && connectivity == Connectivity.Offline,
                        )
                    }
                }
            }
        }

        fun onDownload() = intent {
            startDownload()
        }

        /** Network settings opened on a later try: the earlier failure no longer applies. */
        fun onNetworkSettingsOpened() = intent {
            reduce { state.copy(networkSettingsUnavailable = false) }
        }

        /** Neither the internet panel nor network settings could be opened on this device. */
        fun onNetworkSettingsUnavailable() = intent {
            reduce { state.copy(networkSettingsUnavailable = true) }
        }
    }
