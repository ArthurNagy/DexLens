package com.arthurnagy.dexlens.feature.scanner.impl

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.arthurnagy.dexlens.core.designsystem.AppDimens
import com.arthurnagy.dexlens.core.designsystem.AppSpacing
import com.arthurnagy.dexlens.core.designsystem.CameraColors
import com.arthurnagy.dexlens.core.domain.scan.MatchOutcome
import com.arthurnagy.dexlens.core.model.SpeciesSummary
import com.arthurnagy.dexlens.core.ui.MessageState
import com.arthurnagy.dexlens.core.ui.SpeciesArt
import com.arthurnagy.dexlens.core.ui.TypeChipRow
import com.arthurnagy.dexlens.core.ui.labelRes
import com.arthurnagy.dexlens.core.ui.listText
import com.arthurnagy.dexlens.core.ui.speciesNumberLabel
import com.arthurnagy.dexlens.core.ui.toMessage
import org.orbitmvi.orbit.compose.collectAsState
import com.arthurnagy.dexlens.core.ui.R as CoreUiR

internal enum class CameraPermission { GRANTED, NEEDS_REQUEST, PERMANENTLY_DENIED }

@Composable
internal fun ScannerScreen(
    onNavigateBack: () -> Unit,
    onOpenSpecies: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScannerViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val haptics = LocalHapticFeedback.current

    fun hasCamera() = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    var granted by remember { mutableStateOf(hasCamera()) }
    var permanentlyDenied by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        permanentlyDenied =
            !ok && activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
    }
    LifecycleResumeEffect(Unit) {
        granted = hasCamera()
        if (granted) permanentlyDenied = false
        onPauseOrDispose {}
    }
    LaunchedEffect(state.pendingHaptic) {
        if (state.pendingHaptic) {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            viewModel.onHapticConsumed()
        }
    }

    val permission = when {
        granted -> CameraPermission.GRANTED
        permanentlyDenied -> CameraPermission.PERMANENTLY_DENIED
        else -> CameraPermission.NEEDS_REQUEST
    }
    var torchAvailable by remember { mutableStateOf(false) }
    var cameraFailed by remember { mutableStateOf(false) }

    ScannerContent(
        state = state,
        permission = permission,
        cameraFailed = cameraFailed,
        torchAvailable = torchAvailable,
        debugEnabled = BuildConfig.DEBUG,
        onNavigateBack = onNavigateBack,
        onToggleTorch = viewModel::onTorchToggled,
        onRequestPermission = { launcher.launch(Manifest.permission.CAMERA) },
        onOpenAppSettings = {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
        },
        onOpenSpecies = onOpenSpecies,
        onDismissResult = viewModel::onResultDismissed,
        modifier = modifier,
        camera = { aimBox ->
            val recognizer = remember { MlKitLineRecognizer() }
            DisposableEffect(recognizer) { onDispose { recognizer.close() } }
            val currentBox by rememberUpdatedState(aimBox)
            val currentPaused by rememberUpdatedState(state.paused)
            val analyzer = remember(recognizer) {
                TextFrameAnalyzer(
                    recognizer = recognizer,
                    aimBox = { currentBox },
                    isPaused = { currentPaused },
                    onFrame = { frame -> viewModel.onFrame(frame, debugEnabled = BuildConfig.DEBUG) },
                )
            }
            CameraViewfinder(
                analyzer = analyzer,
                torchOn = state.torchOn,
                onTorchAvailabilityChange = { torchAvailable = it },
                onCameraUnavailable = { cameraFailed = true },
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}

/** Stateless screen body. [camera] is a slot so tests and goldens can run without camera hardware. */
@Composable
internal fun ScannerContent(
    state: ScannerState,
    permission: CameraPermission,
    cameraFailed: Boolean,
    torchAvailable: Boolean,
    debugEnabled: Boolean,
    onNavigateBack: () -> Unit,
    onToggleTorch: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenSpecies: (Int) -> Unit,
    onDismissResult: () -> Unit,
    modifier: Modifier = Modifier,
    camera: @Composable (AimBox) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize().background(CameraColors.background)) {
        val aimBox = remember(constraints.maxWidth, constraints.maxHeight) {
            aimBoxFor(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        }
        when {
            permission != CameraPermission.GRANTED -> {
                PermissionPrompt(permission, onRequestPermission, onOpenAppSettings)
            }

            cameraFailed -> {
                Surface(Modifier.fillMaxSize()) {
                    MessageState(title = stringResource(R.string.scanner_camera_unavailable), modifier = Modifier.safeDrawingPadding())
                }
            }

            state.error != null -> {
                Surface(Modifier.fillMaxSize()) {
                    MessageState(title = state.error.toMessage(), modifier = Modifier.safeDrawingPadding())
                }
            }

            else -> {
                camera(aimBox)
                AimBoxOverlay(aimBox, Modifier.fillMaxSize())
                Text(
                    text = stringResource(R.string.scanner_hint),
                    style = MaterialTheme.typography.titleMedium,
                    color = CameraColors.onCamera,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.xxl)
                        .testTag("scanner:hint"),
                )
                if (debugEnabled) state.debug?.let { DebugOverlay(it, aimBox, Modifier.fillMaxSize()) }
            }
        }
        ScannerToolbar(
            showTorch = permission == CameraPermission.GRANTED && torchAvailable,
            torchOn = state.torchOn,
            onNavigateBack = onNavigateBack,
            onToggleTorch = onToggleTorch,
            modifier = Modifier.align(Alignment.TopCenter).safeDrawingPadding(),
        )
    }
    if (state.locked.isNotEmpty()) {
        ResultSheet(state.locked, onOpenSpecies, onDismissResult)
    }
}

@Composable
private fun ScannerToolbar(
    showTorch: Boolean,
    torchOn: Boolean,
    onNavigateBack: () -> Unit,
    onToggleTorch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = IconButtonDefaults.filledTonalIconButtonColors(
        containerColor = CameraColors.toolbar,
        contentColor = CameraColors.onCamera,
    )
    Row(modifier.fillMaxWidth().padding(AppSpacing.sm), horizontalArrangement = Arrangement.SpaceBetween) {
        FilledTonalIconButton(onClick = onNavigateBack, colors = colors, modifier = Modifier.testTag("scanner:back")) {
            Icon(
                painterResource(CoreUiR.drawable.core_ui_ic_arrow_back),
                contentDescription = stringResource(CoreUiR.string.core_ui_navigate_back),
            )
        }
        if (showTorch) {
            FilledTonalIconButton(onClick = onToggleTorch, colors = colors, modifier = Modifier.testTag("scanner:torch")) {
                Icon(
                    painterResource(if (torchOn) CoreUiR.drawable.core_ui_ic_torch_on else CoreUiR.drawable.core_ui_ic_torch_off),
                    contentDescription = stringResource(if (torchOn) R.string.scanner_torch_off else R.string.scanner_torch_on),
                )
            }
        }
    }
}

@Composable
private fun PermissionPrompt(
    permission: CameraPermission,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDenied = permission == CameraPermission.PERMANENTLY_DENIED
    Surface(modifier.fillMaxSize()) {
        MessageState(
            title = stringResource(R.string.scanner_permission_title),
            message = stringResource(if (isDenied) R.string.scanner_permission_denied else R.string.scanner_permission_rationale),
            actionLabel = stringResource(if (isDenied) R.string.scanner_open_settings else R.string.scanner_allow_camera),
            onAction = if (isDenied) onOpenAppSettings else onRequestPermission,
            modifier = Modifier.safeDrawingPadding(),
        )
    }
}

/** Dims everything outside the aiming box and draws its corner brackets. */
@Composable
private fun AimBoxOverlay(
    box: AimBox,
    modifier: Modifier = Modifier,
) {
    val cornerRadius = with(LocalDensity.current) { AppDimens.aimBoxCorner.toPx() }
    val strokeWidth = with(LocalDensity.current) { AppDimens.aimBoxStroke.toPx() }
    val cornerLength = with(LocalDensity.current) { AppDimens.aimBoxCornerLength.toPx() }
    val description = stringResource(R.string.scanner_aim_box)
    Canvas(
        modifier
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .testTag("scanner:aimBox")
            .clearAndSetSemantics { contentDescription = description },
    ) {
        drawRect(CameraColors.scrim)
        val topLeft = Offset(size.width * box.left, size.height * box.top)
        val boxSize = Size(size.width * (box.right - box.left), size.height * (box.bottom - box.top))
        drawRoundRect(
            CameraColors.background,
            topLeft,
            boxSize,
            CornerRadius(cornerRadius),
            blendMode = BlendMode.Clear,
        )
        drawCornerBrackets(topLeft, boxSize, cornerLength, strokeWidth)
    }
}

private fun DrawScope.drawCornerBrackets(
    topLeft: Offset,
    boxSize: Size,
    length: Float,
    strokeWidth: Float,
) {
    val right = topLeft.x + boxSize.width
    val bottom = topLeft.y + boxSize.height
    val segments = listOf(
        topLeft to Offset(topLeft.x + length, topLeft.y),
        topLeft to Offset(topLeft.x, topLeft.y + length),
        Offset(right, topLeft.y) to Offset(right - length, topLeft.y),
        Offset(right, topLeft.y) to Offset(right, topLeft.y + length),
        Offset(topLeft.x, bottom) to Offset(topLeft.x + length, bottom),
        Offset(topLeft.x, bottom) to Offset(topLeft.x, bottom - length),
        Offset(right, bottom) to Offset(right - length, bottom),
        Offset(right, bottom) to Offset(right, bottom - length),
    )
    segments.forEach { (start, end) -> drawLine(CameraColors.onCamera, start, end, strokeWidth, StrokeCap.Round) }
}

/** Debug builds only: recognised lines in the aiming box and what the matcher decided, for tuning on real screens. */
@Composable
private fun DebugOverlay(
    debug: ScanDebug,
    box: AimBox,
    modifier: Modifier = Modifier,
) {
    val frame = debug.frame
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val left = size.width * box.left
            val top = size.height * box.top
            val scaleX = size.width * (box.right - box.left) / frame.cropWidth
            val scaleY = size.height * (box.bottom - box.top) / frame.cropHeight
            frame.lines.forEach { line ->
                drawRect(
                    color = CameraColors.debugBox,
                    topLeft = Offset(left + line.left * scaleX, top + line.top * scaleY),
                    size = Size((line.right - line.left) * scaleX, (line.bottom - line.top) * scaleY),
                    style = Stroke(width = DEBUG_STROKE),
                )
            }
        }
        Column(
            Modifier
                .safeDrawingPadding()
                .padding(top = AppSpacing.xxl + AppSpacing.xl, start = AppSpacing.sm, end = AppSpacing.sm)
                .background(CameraColors.debugPanel)
                .padding(AppSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        ) {
            Text(
                text = with(frame) { "$bufferWidth×$bufferHeight crop $cropWidth×$cropHeight $processingMillis ms" },
                color = CameraColors.onCamera,
                style = MaterialTheme.typography.labelSmall,
            )
            debug.decisions.take(MAX_DEBUG_LINES).forEach { decision ->
                val color = when (decision.outcome) {
                    MatchOutcome.EXACT, MatchOutcome.CONFUSABLE, MatchOutcome.FUZZY -> CameraColors.debugMatched
                    MatchOutcome.AMBIGUOUS -> CameraColors.debugBox
                    MatchOutcome.IGNORED, MatchOutcome.NO_MATCH -> CameraColors.debugUnmatched
                }
                Text(
                    text = "${decision.outcome}: ${decision.text} ${decision.speciesIds.map { it.value }}",
                    color = color,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultSheet(
    species: List<SpeciesSummary>,
    onOpenSpecies: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        ResultSheetContent(species, onOpenSpecies, onDismiss, Modifier.navigationBarsPadding())
    }
}

@Composable
internal fun ResultSheetContent(
    species: List<SpeciesSummary>,
    onOpenSpecies: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = AppSpacing.md).padding(bottom = AppSpacing.md),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        Text(
            text = stringResource(if (species.size == 1) R.string.scanner_found_one else R.string.scanner_found_several),
            style = MaterialTheme.typography.titleLarge,
        )
        species.forEach { summary -> ResultRow(summary, onClick = { onOpenSpecies(summary.id.value) }) }
        if (species.size == 1) {
            Button(onClick = { onOpenSpecies(species.single().id.value) }, modifier = Modifier.fillMaxWidth().testTag("scanner:open")) {
                Text(stringResource(R.string.scanner_open_entry))
            }
        }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().testTag("scanner:keepScanning")) {
            Text(stringResource(R.string.scanner_keep_scanning))
        }
    }
}

@Composable
private fun ResultRow(
    summary: SpeciesSummary,
    onClick: () -> Unit,
) {
    val number = speciesNumberLabel(summary.id.value)
    val types = listText(summary.types.map { stringResource(it.labelRes()) })
    val description = stringResource(R.string.scanner_result_description, number, summary.name, types)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.cardCorner))
            .clickable(onClick = onClick)
            .testTag("scanner:result:${summary.id.value}")
            .padding(AppSpacing.sm)
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        SpeciesArt(
            name = summary.name,
            types = summary.types,
            imagePath = null,
            contentDescription = null,
            letterStyle = MaterialTheme.typography.titleLarge,
            modifier = Modifier.size(AppDimens.evolutionArt),
        )
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Text(number, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(summary.name, style = MaterialTheme.typography.titleMedium)
            TypeChipRow(summary.types)
        }
    }
}

private const val DEBUG_STROKE = 3f
private const val MAX_DEBUG_LINES = 12
