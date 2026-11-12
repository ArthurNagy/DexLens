package com.arthurnagy.dexlens.feature.settings.impl

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.arthurnagy.dexlens.core.designsystem.AppDimens
import com.arthurnagy.dexlens.core.designsystem.AppSpacing
import com.arthurnagy.dexlens.core.domain.content.ContentPackSizes
import com.arthurnagy.dexlens.core.model.ContentPackState
import com.arthurnagy.dexlens.core.model.ThemeMode
import com.arthurnagy.dexlens.core.ui.messageRes
import org.orbitmvi.orbit.compose.collectAsState
import com.arthurnagy.dexlens.core.ui.R as CoreUiR

@Composable
internal fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val startWithNotifications = rememberNotificationAwareStart(viewModel::onDownload)
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(state.pendingError) {
        val error = state.pendingError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(resources.getString(error.messageRes()))
        viewModel.onErrorConsumed()
    }
    SettingsContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onThemeSelect = viewModel::onThemeSelected,
        onDownload = startWithNotifications,
        onCancelDownload = viewModel::onCancelDownload,
        onDeleteContent = viewModel::onDeleteContent,
        onScannerEnabledChange = viewModel::onScannerEnabledChanged,
        modifier = modifier,
    )
}

/** Asks for the notification that shows download progress if it is not granted yet, then starts either way. */
@Composable
internal fun rememberNotificationAwareStart(onStart: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onStart() }
    return {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else onStart()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsContent(
    state: SettingsState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onThemeSelect: (ThemeMode) -> Unit,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDeleteContent: () -> Unit,
    onScannerEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("settings:back")) {
                        Icon(
                            painterResource(CoreUiR.drawable.core_ui_ic_arrow_back),
                            contentDescription = stringResource(CoreUiR.string.core_ui_navigate_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        // Read once out here: the items then rerun only when their own part changes, not on every download progress step.
        val content = state.content
        val scannerEnabled = state.scannerEnabled
        val themeMode = state.themeMode
        LazyColumn(
            contentPadding = innerPadding,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().testTag("settings:list"),
        ) {
            val sectionModifier = Modifier.widthIn(max = AppDimens.maxContentWidth).fillMaxWidth().padding(AppSpacing.md)
            item(key = "content") {
                Section(stringResource(R.string.settings_content_title), sectionModifier) {
                    ContentPackSection(content, onDownload, onCancelDownload, onDeleteContent)
                }
            }
            item(key = "scanner") {
                Section(stringResource(R.string.settings_scanner), sectionModifier) {
                    ScannerSection(scannerEnabled, onScannerEnabledChange)
                }
            }
            item(key = "theme") {
                Section(stringResource(R.string.settings_theme), sectionModifier) { ThemeSection(themeMode, onThemeSelect) }
            }
            item(key = "about") { Section(stringResource(R.string.settings_about), sectionModifier) { AboutSection() } }
        }
    }
}

@Composable
private fun Section(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        content()
    }
}

/** The download's description, progress and actions. Shared with the first-launch offer. */
@Composable
internal fun ContentPackSection(
    content: ContentPackState,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDeleteContent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Text(stringResource(R.string.settings_content_description), style = MaterialTheme.typography.bodyMedium)
        when (content) {
            ContentPackState.NotDownloaded -> {
                SizeText()
                Button(onClick = onDownload, modifier = Modifier.testTag("settings:download")) {
                    Text(stringResource(R.string.settings_download))
                }
            }

            is ContentPackState.Downloading -> {
                ProgressRow(content.completed, content.total)
                OutlinedButton(onClick = onCancelDownload, modifier = Modifier.testTag("settings:cancel")) {
                    Text(stringResource(R.string.settings_cancel))
                }
            }

            is ContentPackState.Interrupted -> {
                ProgressRow(content.completed, content.total)
                Text(stringResource(R.string.settings_interrupted), style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    Button(onClick = onDownload, modifier = Modifier.testTag("settings:resume")) {
                        Text(stringResource(R.string.settings_resume))
                    }
                    OutlinedButton(onClick = onDeleteContent, modifier = Modifier.testTag("settings:delete")) {
                        Text(stringResource(R.string.settings_delete))
                    }
                }
            }

            ContentPackState.Ready -> {
                Text(stringResource(R.string.settings_ready), style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = onDeleteContent, modifier = Modifier.testTag("settings:delete")) {
                    Text(stringResource(R.string.settings_delete))
                }
            }
        }
    }
}

@Composable
internal fun SizeText(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.settings_size, ContentPackSizes.DOWNLOAD_MB, ContentPackSizes.STORED_MB),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
internal fun ProgressRow(
    completed: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        if (total > 0) {
            LinearProgressIndicator(progress = { completed.toFloat() / total }, modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.settings_progress, completed, total), style = MaterialTheme.typography.bodyMedium)
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ScannerSection(
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = AppDimens.touchTargetMin)
            .toggleable(value = enabled, onValueChange = onChange, role = Role.Switch)
            .testTag("settings:scanner")
            .padding(vertical = AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Text(stringResource(R.string.settings_scanner_switch), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.settings_scanner_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = enabled, onCheckedChange = null)
    }
}

@Composable
private fun ThemeSection(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    Column(Modifier.selectableGroup()) {
        ThemeMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AppDimens.touchTargetMin)
                    .selectable(selected = mode == selected, onClick = { onSelect(mode) }, role = Role.RadioButton)
                    .testTag("settings:theme:$mode")
                    .padding(vertical = AppSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
                RadioButton(selected = mode == selected, onClick = null)
                Text(stringResource(themeLabel(mode)), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

private fun themeLabel(mode: ThemeMode): Int = when (mode) {
    ThemeMode.SYSTEM -> R.string.settings_theme_system
    ThemeMode.LIGHT -> R.string.settings_theme_light
    ThemeMode.DARK -> R.string.settings_theme_dark
}

@Composable
private fun AboutSection() {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Text(stringResource(R.string.settings_credit), style = MaterialTheme.typography.bodyMedium)
        Text(
            stringResource(R.string.settings_legal),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
