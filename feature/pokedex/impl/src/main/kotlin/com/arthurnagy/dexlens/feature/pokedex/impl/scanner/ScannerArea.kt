package com.arthurnagy.dexlens.feature.pokedex.impl.scanner

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import com.arthurnagy.dexlens.core.designsystem.AppDimens
import com.arthurnagy.dexlens.core.designsystem.AppSpacing
import com.arthurnagy.dexlens.core.designsystem.CameraColors
import com.arthurnagy.dexlens.core.designsystem.DarkScheme
import com.arthurnagy.dexlens.core.domain.scan.MatchOutcome
import com.arthurnagy.dexlens.core.model.SpeciesSummary
import com.arthurnagy.dexlens.core.ui.MessageState
import com.arthurnagy.dexlens.core.ui.listText
import com.arthurnagy.dexlens.core.ui.toMessage
import com.arthurnagy.dexlens.feature.pokedex.impl.R
import com.arthurnagy.dexlens.core.ui.R as CoreUiR

internal enum class CameraPermission { GRANTED, NEEDS_REQUEST, PERMANENTLY_DENIED }

/** What the device allows the scanner to do. */
@Immutable
internal data class CameraStatus(
    val permission: CameraPermission,
    val failed: Boolean,
    val torchAvailable: Boolean,
)

/** 3:2 below the status bar, capped in short windows so the list stays reachable. */
internal fun scannerAreaHeight(
    width: Dp,
    height: Dp,
    topInset: Dp,
): Dp = min(width / AppDimens.scannerAreaAspectRatio + topInset, height * AppDimens.scannerAreaMaxHeightFraction)

/** How far the camera area has faded into the top bar, from 0 with the sheet collapsed to 1 with it expanded. */
internal fun topBarAlpha(
    sheetOffset: Float,
    collapsedOffset: Float,
    expandedOffset: Float,
): Float = ((collapsedOffset - sheetOffset) / (collapsedOffset - expandedOffset).coerceAtLeast(1f)).coerceIn(0f, 1f)

/**
 * The top of the home screen. [viewfinder] is a slot so tests and goldens can run without camera hardware, and it is
 * composed only while [cameraRunning], because leaving composition is what turns the camera off.
 */
@Composable
internal fun ScannerArea(
    scan: ScanState,
    camera: CameraStatus,
    cameraRunning: Boolean,
    topBarAlpha: () -> Float,
    height: Dp,
    topInset: Dp,
    debugEnabled: Boolean,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewfinder: @Composable (AimBox) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth().height(height).clipToBounds().background(CameraColors.background)) {
        val areaWidth = maxWidth
        val areaHeight = maxHeight
        val density = LocalDensity.current
        val aimBox = remember(constraints.maxWidth, constraints.maxHeight, topInset, density) {
            aimBoxFor(
                width = constraints.maxWidth.toFloat(),
                height = constraints.maxHeight.toFloat(),
                topInset = with(density) { topInset.toPx() },
                reservedWidth = with(density) { AppDimens.scannerButtonRowWidth.toPx() },
            )
        }
        when {
            camera.permission != CameraPermission.GRANTED -> {
                PermissionPrompt(camera.permission, topInset, onRequestPermission, onOpenAppSettings)
            }

            camera.failed -> {
                ScannerMessage(stringResource(R.string.pokedex_scanner_camera_unavailable), topInset)
            }

            scan.error != null -> {
                ScannerMessage(scan.error.toMessage(), topInset)
            }

            cameraRunning -> {
                viewfinder(aimBox)
                AimBoxOverlay(aimBox, Modifier.fillMaxSize())
                ScanLabel(
                    match = scan.match,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            start = areaWidth * aimBox.left + AppSpacing.sm,
                            end = areaWidth * (1f - aimBox.right) + AppSpacing.sm,
                            bottom = areaHeight * (1f - aimBox.bottom) + AppSpacing.sm,
                        ),
                )
                if (debugEnabled) scan.debug?.let { DebugOverlay(it, aimBox, topInset, Modifier.fillMaxSize()) }
            }
        }
        // Read while drawing, so following the sheet doesn't recompose the camera on every frame.
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = topBarAlpha() }
                .background(MaterialTheme.colorScheme.surface),
        )
    }
}

/** The hint, or the matched names in its place. Screen readers announce the change. */
@Composable
private fun ScanLabel(
    match: List<SpeciesSummary>,
    modifier: Modifier = Modifier,
) {
    Text(
        text = if (match.isEmpty()) stringResource(R.string.pokedex_scanner_hint) else listText(match.map { it.name }),
        style = MaterialTheme.typography.titleSmall,
        color = CameraColors.onCamera,
        textAlign = TextAlign.Center,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(CameraColors.toolbar, RoundedCornerShape(AppDimens.aimBoxCorner))
            .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag("pokedex:scanLabel"),
    )
}

/** The torch, while the camera can use it, and settings. Sits above the sheet, so settings stays in both positions. */
@Composable
internal fun ScannerButtons(
    showTorch: Boolean,
    torchOn: Boolean,
    onTopBar: Boolean,
    topInset: Dp,
    onToggleTorch: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(if (onTopBar) MaterialTheme.colorScheme.surface else CameraColors.toolbar)
    val contentColor by animateColorAsState(if (onTopBar) MaterialTheme.colorScheme.onSurface else CameraColors.onCamera)
    val colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = containerColor, contentColor = contentColor)
    Row(
        modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.End))
            .padding(top = topInset, end = AppSpacing.sm)
            .height(AppDimens.scannerButtonRowHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showTorch) {
            FilledTonalIconButton(onClick = onToggleTorch, colors = colors, modifier = Modifier.testTag("pokedex:torch")) {
                Icon(
                    painterResource(if (torchOn) CoreUiR.drawable.core_ui_ic_torch_on else CoreUiR.drawable.core_ui_ic_torch_off),
                    contentDescription = stringResource(
                        if (torchOn) R.string.pokedex_scanner_torch_off else R.string.pokedex_scanner_torch_on,
                    ),
                )
            }
        }
        FilledTonalIconButton(onClick = onOpenSettings, colors = colors, modifier = Modifier.testTag("pokedex:settings")) {
            Icon(painterResource(CoreUiR.drawable.core_ui_ic_settings), contentDescription = stringResource(R.string.pokedex_settings))
        }
    }
}

@Composable
private fun PermissionPrompt(
    permission: CameraPermission,
    topInset: Dp,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    val isDenied = permission == CameraPermission.PERMANENTLY_DENIED
    ScannerMessage(
        title = stringResource(R.string.pokedex_scanner_permission_title),
        topInset = topInset,
        message = stringResource(
            if (isDenied) R.string.pokedex_scanner_permission_denied else R.string.pokedex_scanner_permission_rationale,
        ),
        actionLabel = stringResource(if (isDenied) R.string.pokedex_scanner_open_settings else R.string.pokedex_scanner_allow_camera),
        onAction = if (isDenied) onOpenAppSettings else onRequestPermission,
    )
}

/** A message on the area's dark background in every theme. It scrolls, so it stays reachable in a capped, short area. */
@Composable
private fun ScannerMessage(
    title: String,
    topInset: Dp,
    message: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    // MessageState adds its own padding, so only what's left of the button row is added here.
    val top = max(topInset + AppDimens.scannerButtonRowHeight - AppSpacing.xl, AppSpacing.none)
    MaterialTheme(colorScheme = DarkScheme) {
        CompositionLocalProvider(LocalContentColor provides CameraColors.onCamera) {
            Box(Modifier.fillMaxSize().padding(top = top), contentAlignment = Alignment.Center) {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    MessageState(title = title, message = message, actionLabel = actionLabel, onAction = onAction)
                }
            }
        }
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
    val description = stringResource(R.string.pokedex_scanner_aim_box)
    Canvas(
        modifier
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .testTag("pokedex:aimBox")
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
        drawCornerBrackets(topLeft, boxSize, cornerRadius, cornerLength, strokeWidth)
    }
}

/** Each bracket runs along one side, turns the box's rounded corner and runs along the other. */
private fun DrawScope.drawCornerBrackets(
    topLeft: Offset,
    boxSize: Size,
    radius: Float,
    length: Float,
    strokeWidth: Float,
) {
    val left = topLeft.x
    val top = topLeft.y
    val right = left + boxSize.width
    val bottom = top + boxSize.height
    val diameter = radius * 2f
    val brackets = Path().apply {
        moveTo(left, top + length)
        arcTo(Rect(left, top, left + diameter, top + diameter), 180f, 90f, forceMoveTo = false)
        lineTo(left + length, top)

        moveTo(right - length, top)
        arcTo(Rect(right - diameter, top, right, top + diameter), 270f, 90f, forceMoveTo = false)
        lineTo(right, top + length)

        moveTo(right, bottom - length)
        arcTo(Rect(right - diameter, bottom - diameter, right, bottom), 0f, 90f, forceMoveTo = false)
        lineTo(right - length, bottom)

        moveTo(left + length, bottom)
        arcTo(Rect(left, bottom - diameter, left + diameter, bottom), 90f, 90f, forceMoveTo = false)
        lineTo(left, bottom - length)
    }
    drawPath(brackets, CameraColors.onCamera, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
}

/** Debug builds only: recognised lines in the aiming box and what the matcher decided, for tuning on real screens. */
@Composable
private fun DebugOverlay(
    debug: ScanDebug,
    box: AimBox,
    topInset: Dp,
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
                .padding(top = topInset + AppDimens.scannerButtonRowHeight, start = AppSpacing.sm, end = AppSpacing.sm)
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

private const val DEBUG_STROKE = 3f
private const val MAX_DEBUG_LINES = 12
