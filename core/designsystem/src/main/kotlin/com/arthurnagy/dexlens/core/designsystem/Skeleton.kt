package com.arthurnagy.dexlens.core.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Sweeps one highlight across every [SkeletonBlock] inside [content], so a whole page shimmers in step. The animation is
 * only read while drawing, so it never recomposes the page.
 */
@Composable
fun SkeletonShimmer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val highlight = MaterialTheme.colorScheme.surfaceBright.copy(alpha = HIGHLIGHT_ALPHA)
    val sweep by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(SWEEP_MILLIS, easing = LinearEasing)),
        label = "skeleton sweep",
    )
    Box(
        modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val band = size.width * BAND_FRACTION
                val start = (size.width + band) * sweep - band
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, highlight, Color.Transparent),
                        start = Offset(start, 0f),
                        end = Offset(start + band, 0f),
                    ),
                    blendMode = BlendMode.SrcAtop,
                )
            },
    ) {
        content()
    }
}

/** A placeholder for a piece of content that is still loading. Size it through [modifier]. */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AppDimens.skeletonLineHeight / 2),
) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest, shape))
}

private const val HIGHLIGHT_ALPHA = 0.8f
private const val BAND_FRACTION = 0.6f
private const val SWEEP_MILLIS = 1200
