package com.arthurnagy.dexlens.feature.settings.impl

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.arthurnagy.dexlens.core.domain.content.MarkContentOfferShownUseCase
import com.arthurnagy.dexlens.core.domain.content.ObserveContentPackStateUseCase
import com.arthurnagy.dexlens.core.domain.content.StartContentDownloadUseCase
import com.arthurnagy.dexlens.core.model.ContentPackState
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer
import javax.inject.Inject

@Immutable
internal data class ContentOfferState(
    val content: ContentPackState,
) {
    companion object {
        val Initial = ContentOfferState(ContentPackState.NotDownloaded)
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
    ) : ViewModel(),
        OrbitContainerHost<ContentOfferState, ContentOfferState, Nothing> {
        override val container = orbitContainer<ContentOfferState, Nothing>(ContentOfferState.Initial) {
            markAnswered()
            observeState()
        }

        internal fun markAnswered() = intent {
            markOfferShown()
        }

        private fun observeState() = intent {
            observeContent().collect { content -> reduce { state.copy(content = content) } }
        }

        fun onDownload() = intent {
            startDownload()
        }
    }
