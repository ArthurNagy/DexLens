package com.arthurnagy.dexlens.feature.pokedex.impl.scanner

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
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
import com.arthurnagy.dexlens.core.domain.DomainError
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
 * The top of the home screen, [height] tall where it can be seen. [viewfinder] is a slot so tests and goldens can run
 * without camera hardware, and it is composed only while [cameraRunning], because leaving composition is what turns the
 * camera off.
 *
 * Where the camera picture is sure to reach past the area by the sheet's [cornerRadius] above and below at its current
 * scale ([cornersFit], given [cameraUprightPortrait]), the camera, its overlay and the fade carry on that far under the
 * sheet, so its rounded corners show the camera. The viewfinder then grows by the radius at both ends, keeping its
 * centre and scale, so the aiming box covers the same sensor pixels. Everything the user reads or taps stays laid out
 * in the visible [height].
 */
@Composable
internal fun ScannerArea(
    match: List<SpeciesSummary>,
    scanError: DomainError?,
    camera: CameraStatus,
    cameraRunning: Boolean,
    topBarAlpha: () -> Float,
    height: Dp,
    cornerRadius: Dp,
    cameraUprightPortrait: Boolean?,
    topInset: Dp,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewfinder: @Composable (AimBox) -> Unit,
) {
    val density = LocalDensity.current
    val heightPx = with(density) { height.roundToPx() }
    val radiusPx = with(density) { cornerRadius.roundToPx() }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widthPx = constraints.maxWidth
        // Recomputed on every composition: the same window always gives the same answer, so the camera binds once.
        val underlapPx = if (cornersFit(widthPx, heightPx, radiusPx, cameraUprightPortrait)) radiusPx else 0
        val aimBox = remember(widthPx, heightPx, topInset, density) {
            aimBoxFor(
                width = widthPx.toFloat(),
                height = heightPx.toFloat(),
                topInset = with(density) { topInset.toPx() },
                reservedWidth = with(density) { AppDimens.scannerButtonRowWidth.toPx() },
                buttonRowHeight = with(density) { AppDimens.scannerButtonRowHeight.toPx() },
                margin = with(density) { AppSpacing.md.toPx() },
            )
        }
        val cameraBox = remember(aimBox, heightPx, underlapPx) {
            aimBox.inViewfinder(heightPx.toFloat(), underlapPx.toFloat(), underlapPx.toFloat())
        }
        val showCamera = camera.permission == CameraPermission.GRANTED && !camera.failed && scanError == null && cameraRunning
        Box(
            Modifier
                .fillMaxWidth()
                .height(with(density) { (heightPx + underlapPx).toDp() })
                .clipToBounds()
                .background(CameraColors.background),
        ) {
            if (showCamera) {
                Box(Modifier.fillMaxWidth().viewfinderLayer(heightPx, underlapPx)) {
                    viewfinder(cameraBox)
                    AimBoxOverlay(cameraBox, Modifier.fillMaxSize())
                }
            }
            ScannerContent(
                match = match,
                scanError = scanError,
                camera = camera,
                showCamera = showCamera,
                aimBox = aimBox,
                topInset = topInset,
                onRequestPermission = onRequestPermission,
                onOpenAppSettings = onOpenAppSettings,
                modifier = Modifier.fillMaxWidth().height(with(density) { heightPx.toDp() }),
            )
            // Read while drawing, so following the sheet doesn't recompose the camera on every frame.
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = topBarAlpha() }
                    .background(MaterialTheme.colorScheme.surfaceContainer),
            )
        }
    }
}

/** Measures the camera layer exactly [underlapPx] taller at both ends than the visible [heightPx], from [underlapPx] above. */
private fun Modifier.viewfinderLayer(
    heightPx: Int,
    underlapPx: Int,
): Modifier = layout { measurable, constraints ->
    val layerHeight = heightPx + 2 * underlapPx
    val placeable = measurable.measure(constraints.copy(minHeight = layerHeight, maxHeight = layerHeight))
    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(0, -underlapPx) }
}

/** What the user reads or taps in the visible part of the area: the label, the messages and the aiming box's description. */
@Composable
private fun ScannerContent(
    match: List<SpeciesSummary>,
    scanError: DomainError?,
    camera: CameraStatus,
    showCamera: Boolean,
    aimBox: AimBox,
    topInset: Dp,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.testTag("pokedex:scannerArea")) {
        val areaWidth = maxWidth
        val areaHeight = maxHeight
        when {
            camera.permission != CameraPermission.GRANTED -> {
                PermissionPrompt(camera.permission, topInset, onRequestPermission, onOpenAppSettings)
            }

            camera.failed -> {
                ScannerMessage(stringResource(R.string.pokedex_scanner_camera_unavailable), topInset)
            }

            scanError != null -> {
                ScannerMessage(scanError.toMessage(), topInset)
            }

            showCamera -> {
                AimBoxDescription(
                    Modifier
                        .padding(start = areaWidth * aimBox.left, top = areaHeight * aimBox.top)
                        .size(width = areaWidth * (aimBox.right - aimBox.left), height = areaHeight * (aimBox.bottom - aimBox.top)),
                )
                ScanLabel(
                    match = match,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            start = areaWidth * aimBox.left + AppSpacing.sm,
                            end = areaWidth * (1f - aimBox.right) + AppSpacing.sm,
                            bottom = areaHeight * (1f - aimBox.bottom) + AppSpacing.sm,
                        ),
                )
            }
        }
    }
}

/**
 * The aiming box for screen readers, sized to the box the user sees. The drawn box reaches under the sheet with the
 * camera, so its description lives here instead, where focus can't land under the sheet.
 */
@Composable
private fun AimBoxDescription(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.pokedex_scanner_aim_box)
    Box(modifier.testTag("pokedex:aimBox").semantics { contentDescription = description })
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
    val containerColor by animateColorAsState(if (onTopBar) MaterialTheme.colorScheme.surfaceContainer else CameraColors.toolbar)
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

/**
 * The app's name at the start of the top bar. [alpha] follows the camera's fade into the bar, and screen readers find
 * the title only while it is [visible], so it isn't announced over the camera.
 */
@Composable
internal fun TopBarTitle(
    visible: Boolean,
    topInset: Dp,
    modifier: Modifier = Modifier,
    alpha: () -> Float = { 1f },
) {
    Text(
        text = stringResource(R.string.pokedex_title),
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start))
            .padding(top = topInset, start = AppSpacing.md)
            .height(AppDimens.scannerButtonRowHeight)
            .wrapContentHeight(Alignment.CenterVertically)
            .graphicsLayer { this.alpha = alpha() }
            .testTag("pokedex:title")
            .then(if (visible) Modifier.semantics { heading() } else Modifier.clearAndSetSemantics {}),
    )
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

/** Dims everything outside the aiming box and draws its corner brackets. Drawing only: [AimBoxDescription] describes it. */
@Composable
private fun AimBoxOverlay(
    box: AimBox,
    modifier: Modifier = Modifier,
) {
    val cornerRadius = with(LocalDensity.current) { AppDimens.aimBoxCorner.toPx() }
    val strokeWidth = with(LocalDensity.current) { AppDimens.aimBoxStroke.toPx() }
    val cornerLength = with(LocalDensity.current) { AppDimens.aimBoxCornerLength.toPx() }
    Canvas(modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
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
