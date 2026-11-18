package com.arthurnagy.dexlens.feature.settings.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arthurnagy.dexlens.core.designsystem.AppDimens
import com.arthurnagy.dexlens.core.designsystem.AppSpacing
import com.arthurnagy.dexlens.core.model.Connectivity
import com.arthurnagy.dexlens.core.model.ContentPackState
import org.orbitmvi.orbit.compose.collectAsState

/** The first-launch offer, shown as a dialog scene over the list. */
@Composable
internal fun ContentOfferDialog(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContentOfferViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val start = rememberNotificationAwareStart(viewModel::onDownload)
    // One outcome callback per tap, so a failed retry can't race a "cleared" update and hide the message.
    val openNetworkSettings = rememberOpenNetworkSettings(
        onUnavailable = viewModel::onNetworkSettingsUnavailable,
        onOpen = viewModel::onNetworkSettingsOpened,
    )
    ContentOfferContent(
        state = state.content,
        connectivity = state.connectivity,
        networkSettingsUnavailable = state.networkSettingsUnavailable,
        onDownload = start,
        onConnect = openNetworkSettings,
        onSkip = onClose,
        onHide = onClose,
        modifier = modifier,
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ContentOfferContent(
    state: ContentPackState,
    connectivity: Connectivity,
    networkSettingsUnavailable: Boolean,
    onDownload: () -> Unit,
    onConnect: () -> Unit,
    onSkip: () -> Unit,
    onHide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // A dialog is its own window, so the app root's testTagsAsResourceId doesn't reach it: set again for UI Automator.
    Surface(
        modifier = modifier.semantics { testTagsAsResourceId = true },
        shape = RoundedCornerShape(AppDimens.cardCorner),
        tonalElevation = AppSpacing.xs,
    ) {
        Column(Modifier.padding(AppSpacing.lg), verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
            Text(stringResource(R.string.settings_offer_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.settings_content_description), style = MaterialTheme.typography.bodyMedium)
            when (state) {
                ContentPackState.NotDownloaded -> {
                    SizeText()
                    if (connectivity == Connectivity.Online) {
                        Button(onClick = onDownload, modifier = Modifier.fillMaxWidth().testTag("offer:download")) {
                            Text(stringResource(R.string.settings_download))
                        }
                    } else {
                        OfflineText()
                        Button(onClick = onConnect, modifier = Modifier.fillMaxWidth().testTag("offer:connect")) {
                            Text(stringResource(R.string.settings_connect))
                        }
                        if (networkSettingsUnavailable) {
                            Text(
                                text = stringResource(R.string.settings_network_settings_unavailable),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .testTag("offer:message")
                                    .semantics { liveRegion = LiveRegionMode.Polite },
                            )
                        }
                    }
                    TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth().testTag("offer:skip")) {
                        Text(stringResource(R.string.settings_offer_skip))
                    }
                }

                is ContentPackState.Downloading -> {
                    ProgressRow(state.completed, state.total)
                    TextButton(onClick = onHide, modifier = Modifier.fillMaxWidth().testTag("offer:hide")) {
                        Text(stringResource(R.string.settings_offer_background))
                    }
                }

                is ContentPackState.Interrupted -> {
                    ProgressRow(state.completed, state.total)
                    Button(onClick = onDownload, modifier = Modifier.fillMaxWidth().testTag("offer:resume")) {
                        Text(stringResource(R.string.settings_resume))
                    }
                    TextButton(onClick = onHide, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.settings_offer_later)) }
                }

                ContentPackState.Ready -> {
                    Text(stringResource(R.string.settings_ready), style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = onHide, modifier = Modifier.fillMaxWidth().testTag("offer:done")) {
                        Text(stringResource(R.string.settings_offer_done))
                    }
                }
            }
        }
    }
}
