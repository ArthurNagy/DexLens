package com.arthurnagy.dexlens.feature.pokedex.impl

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.IntOffset
import com.arthurnagy.dexlens.core.designsystem.AppDimens
import com.arthurnagy.dexlens.core.designsystem.AppSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A fast-scroll thumb over the list that steps by generation, drawn over [listState]'s list and filling it.
 *
 * - The thumb fades in when the list's position changes and out [FADE_DELAY_MS] after scrolling and dragging stop. A
 *   drag that only moves the sheet leaves the position alone, so it doesn't show the thumb.
 * - Dragging it jumps to each generation's first species. The bubble and the ticks show only while the finger is down.
 * - Its track is the part of the list visible in the window, so a collapsed sheet's off-screen rows are left out.
 * - Screen readers get an adjustable slider that is there even while the thumb is hidden.
 */
@Composable
internal fun GenerationScroller(
    listState: LazyListState,
    slots: List<GenerationSlot>,
    modifier: Modifier = Modifier,
) {
    val count = slots.size
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val topInsetPx = with(density) { AppSpacing.xs.toPx() }
    val bottomInsetPx = WindowInsets.navigationBars.getBottom(density) + with(density) { AppSpacing.md.toPx() }
    val targetPx = with(density) { AppDimens.touchTargetMin.toPx() }

    // Read in layout and draw only, so the sheet moving re-places the thumb without recomposing.
    var track by remember { mutableStateOf(Track.Empty) }
    var visible by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var draggedSlot by remember { mutableIntStateOf(0) }
    val topSlot by remember(slots) { derivedStateOf { slotOfIndex(listState.firstVisibleItemIndex, slots) } }
    val shownSlot = if (dragging) draggedSlot else topSlot

    fun jump(slot: Int) {
        scope.launch { listState.scrollToItem(slots[slot].firstIndex) }
    }

    LaunchedEffect(listState, slots) {
        merge(
            snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }.drop(1).map { },
            snapshotFlow { dragging }.drop(1).map { },
        ).collectLatest {
            visible = true
            if (dragging) return@collectLatest
            delay(FADE_DELAY_MS)
            visible = false
        }
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(if (visible) FADE_IN_MS else FADE_OUT_MS),
        label = "fastScrollAlpha",
    )

    Box(
        modifier.onGloballyPositioned { coordinates ->
            val origin = coordinates.positionInWindow()
            val shown = coordinates.boundsInWindow()
            val top = shown.top - origin.y + topInsetPx
            val bottom = shown.bottom - origin.y - bottomInsetPx
            val measured = Track(top, max(0f, bottom - top))
            if (measured != track) track = measured
        },
    ) {
        if (dragging) DragOverlay(slots, shownSlot, track)

        val stateText = generationLabel(slots[topSlot].generation, R.string.pokedex_generation_state)
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset { IntOffset(0, (slotCentre(shownSlot, track.top, track.height, count) - targetPx / 2).roundToInt()) }
                .size(AppDimens.touchTargetMin)
                .testTag("pokedex:fastScroll:control")
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(topSlot + 1f, 1f..count.toFloat(), steps = max(0, count - 2))
                    stateDescription = stateText
                    traversalIndex = -1f
                    setProgress { value ->
                        jump((value.roundToInt() - 1).coerceIn(0, count - 1))
                        true
                    }
                }.then(
                    if (visible) {
                        Modifier.pointerInput(slots) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                down.consume()
                                // In track space: this target moves as it snaps, so its own coordinates would make the slot jitter.
                                var fingerY = slotCentre(topSlot, track.top, track.height, count) + down.position.y - size.height / 2f
                                draggedSlot = topSlot
                                dragging = true
                                try {
                                    drag(down.id) { change ->
                                        fingerY += change.positionChange().y
                                        change.consume()
                                        val slot = slotAt(fingerY, track.top, track.height, count)
                                        if (slot != draggedSlot) {
                                            draggedSlot = slot
                                            jump(slot)
                                        }
                                    }
                                } finally {
                                    dragging = false
                                }
                            }
                        }
                    } else {
                        Modifier
                    },
                ),
        ) {
            if (visible || alpha > 0f) {
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = AppSpacing.xs)
                        .size(AppDimens.fastScrollThumbWidth, AppDimens.fastScrollThumbHeight)
                        .graphicsLayer { this.alpha = alpha }
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .testTag("pokedex:fastScroll:thumb"),
                )
            }
        }
    }
}

/** The track with its tick marks, and the bubble naming the generation under the finger. */
@Composable
private fun BoxScope.DragOverlay(
    slots: List<GenerationSlot>,
    shownSlot: Int,
    track: Track,
) {
    val count = slots.size
    val density = LocalDensity.current
    val labelLinePx = with(density) { MaterialTheme.typography.labelSmall.lineHeight.toPx() }
    val allLabels = track.height / count >= labelLinePx
    Box(
        Modifier
            .align(Alignment.TopEnd)
            .offset { IntOffset(0, track.top.roundToInt()) }
            .size(AppDimens.fastScrollTrackWidth, with(density) { track.height.toDp() })
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
    )
    slots.forEachIndexed { index, slot ->
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = AppSpacing.md)
                .centredOn(top = 0f, bottom = Float.MAX_VALUE) { slotCentre(index, track.top, track.height, count) }
                .testTag("pokedex:fastScroll:tick:${slot.generation}"),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Odd numbers only when nine labels would overlap; every dot still shows.
            if (allLabels || slot.generation % 2 == 1) {
                Text(
                    slot.generation.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                Modifier
                    .size(AppDimens.fastScrollTickDot)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape),
            )
        }
    }
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(end = AppDimens.touchTargetMin + AppSpacing.sm)
            .centredOn(top = track.top, bottom = track.top + track.height) {
                slotCentre(shownSlot, track.top, track.height, count)
            }.testTag("pokedex:fastScroll:bubble"),
    ) {
        Text(
            generationLabel(slots[shownSlot].generation, R.string.pokedex_generation_bubble),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
        )
    }
}

/** "Generation N" with its region through [withRegion], or the number alone for a generation with no region name. */
@Composable
private fun generationLabel(
    generation: Int,
    @StringRes withRegion: Int,
): String {
    val region = regionRes(generation)
    return if (region != null) {
        stringResource(withRegion, generation, stringResource(region))
    } else {
        stringResource(R.string.pokedex_generation_number, generation)
    }
}

/** Places the content vertically centred on [centre], kept between [top] and [bottom] where it fits. */
private fun Modifier.centredOn(
    top: Float,
    bottom: Float,
    centre: () -> Float,
): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minHeight = 0))
    layout(placeable.width, placeable.height) {
        val y = (centre() - placeable.height / 2f).coerceAtMost(bottom - placeable.height).coerceAtLeast(top)
        placeable.placeRelative(0, y.roundToInt())
    }
}

/** The thumb's track in the scroller's own coordinates: the part of the list visible in the window. */
private data class Track(
    val top: Float,
    val height: Float,
) {
    companion object {
        val Empty = Track(0f, 0f)
    }
}

private const val FADE_DELAY_MS = 1_500L
private const val FADE_IN_MS = 150
private const val FADE_OUT_MS = 300
