package com.arthurnagy.dexlens.feature.settings.impl

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.arthurnagy.dexlens.core.designsystem.AppDimens
import com.arthurnagy.dexlens.core.designsystem.AppSpacing
import com.arthurnagy.dexlens.core.ui.MessageState
import com.arthurnagy.dexlens.core.ui.messageRes
import org.orbitmvi.orbit.compose.collectAsState
import com.arthurnagy.dexlens.core.ui.R as CoreUiR

/** One library's copyright holder, licence texts and bundled notices, the detail pane beside the licences list. */
@Composable
internal fun LicenseDetailScreen(
    viewModel: LicenseDetailViewModel,
    onNavigateBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(state.pendingError) {
        val error = state.pendingError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(resources.getString(error.messageRes()))
        viewModel.onErrorConsumed()
    }
    LicenseDetailContent(state, snackbarHostState, onNavigateBack, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LicenseDetailContent(
    state: LicenseDetailState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("licence:back")) {
                            Icon(
                                painterResource(CoreUiR.drawable.core_ui_ic_arrow_back),
                                contentDescription = stringResource(CoreUiR.string.core_ui_navigate_back),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.testTag("licence:loading"))
            }

            state.notFound -> MessageState(
                title = stringResource(R.string.settings_library_not_found),
                modifier = Modifier.padding(innerPadding).testTag("licence:not-found"),
            )

            // Loading failed for another reason: the snackbar said so, and this stays once it goes.
            state.name == null -> MessageState(
                title = stringResource(R.string.settings_licences_unavailable),
                modifier = Modifier.padding(innerPadding).testTag("licence:unavailable"),
            )

            else -> LicenceRows(state, innerPadding)
        }
    }
}

@Composable
private fun LicenceRows(
    state: LicenseDetailState,
    innerPadding: PaddingValues,
) {
    val rows = state.rows
    LazyColumn(
        contentPadding = innerPadding,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().testTag("licence:rows"),
    ) {
        val rowModifier = Modifier.widthIn(max = AppDimens.maxContentWidth).fillMaxWidth().padding(horizontal = AppSpacing.md)
        item(key = "header") {
            Column(rowModifier.padding(bottom = AppSpacing.md)) {
                Text(state.name.orEmpty(), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                state.version?.let { version ->
                    Text(
                        stringResource(R.string.settings_version, version),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("licence:version"),
                    )
                }
            }
        }
        items(rows.size, key = { index -> index }, contentType = { index -> rows[index]::class }) { index ->
            when (val row = rows[index]) {
                is LicenceRow.Holder -> Text(
                    stringResource(R.string.settings_copyright, row.text),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = rowModifier.padding(bottom = AppSpacing.sm),
                )

                is LicenceRow.SectionTitle -> Text(
                    row.text,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = rowModifier.padding(top = AppSpacing.md, bottom = AppSpacing.sm).semantics { heading() },
                )

                is LicenceRow.Paragraph -> Text(
                    row.text,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = rowModifier.padding(bottom = AppSpacing.sm),
                )
            }
        }
    }
}
