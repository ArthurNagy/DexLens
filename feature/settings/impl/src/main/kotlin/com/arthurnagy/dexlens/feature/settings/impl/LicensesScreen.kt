package com.arthurnagy.dexlens.feature.settings.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arthurnagy.dexlens.core.designsystem.AppSpacing
import com.arthurnagy.dexlens.core.model.OpenSourceLibrary
import com.arthurnagy.dexlens.core.ui.MessageState
import com.arthurnagy.dexlens.core.ui.listText
import com.arthurnagy.dexlens.core.ui.messageRes
import org.orbitmvi.orbit.compose.collectAsState
import com.arthurnagy.dexlens.core.ui.R as CoreUiR

/** The libraries bundled in the app, the list pane beside a library's licence. [onNavigateBack] null hides Back. */
@Composable
internal fun LicensesScreen(
    onNavigateBack: (() -> Unit)?,
    onOpenLibrary: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LicensesViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(state.pendingError) {
        val error = state.pendingError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(resources.getString(error.messageRes()))
        viewModel.onErrorConsumed()
    }
    LicensesContent(state, snackbarHostState, onNavigateBack, onOpenLibrary, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LicensesContent(
    state: LicensesState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: (() -> Unit)?,
    onOpenLibrary: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_licences)) },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("licenses:back")) {
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
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.testTag("licenses:loading"))
            }
        } else if (state.libraries.isEmpty()) {
            // Loading failed: the snackbar said so, and this stays once it goes.
            MessageState(
                title = stringResource(R.string.settings_licences_unavailable),
                modifier = Modifier.padding(innerPadding).testTag("licenses:unavailable"),
            )
        } else {
            LazyColumn(contentPadding = innerPadding, modifier = Modifier.fillMaxSize().testTag("licenses:list")) {
                items(state.libraries, key = { it.id }) { library -> LibraryRow(library, onOpenLibrary) }
            }
        }
    }
}

@Composable
private fun LibraryRow(
    library: OpenSourceLibrary,
    onOpen: (String) -> Unit,
) {
    val licences = listText(library.licenseNames)
    val version = library.version
    ListItem(
        headlineContent = { Text(library.name) },
        supportingContent = {
            Text(
                if (version != null) stringResource(R.string.settings_library_summary, version, licences) else licences,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = Modifier
            .clickable(role = Role.Button) { onOpen(library.id) }
            .testTag("licenses:library:${library.id}"),
    )
}

/** Shown beside the list on a wide window until a library is picked. */
@Composable
internal fun LicencePlaceholder(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(AppSpacing.xl), contentAlignment = Alignment.Center) {
        Text(
            stringResource(R.string.settings_licence_placeholder),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("licenses:placeholder"),
        )
    }
}
