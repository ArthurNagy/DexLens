package com.arthurnagy.dexlens.core.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection

/** One base-stat row: a label, its value and a bar out of [max]. [description] is what screen readers announce. */
@Composable
fun StatBar(
    label: String,
    value: Int,
    color: Color,
    description: String,
    modifier: Modifier = Modifier,
    max: Int = MAX_BASE_STAT,
) {
    val fraction by animateFloatAsState(targetValue = (value.toFloat() / max).coerceIn(0f, 1f), label = "stat")
    Row(
        modifier = modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        Text(label, Modifier.widthIn(min = AppDimens.statLabelWidth), style = MaterialTheme.typography.labelLarge)
        Text(
            value.toString(),
            Modifier.widthIn(min = AppDimens.statValueWidth),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.End,
        )
        val track = MaterialTheme.colorScheme.surfaceContainerHighest
        // The fill is drawn, not laid out: the animation then only redraws the bar instead of recomposing the row.
        Box(
            Modifier
                .weight(1f)
                .height(AppDimens.statBarHeight)
                .drawBehind {
                    val radius = CornerRadius(size.height / 2)
                    drawRoundRect(track, cornerRadius = radius)
                    val width = size.width * fraction
                    val start = if (layoutDirection == LayoutDirection.Rtl) size.width - width else 0f
                    drawRoundRect(color, topLeft = Offset(start, 0f), size = Size(width, size.height), cornerRadius = radius)
                },
        )
    }
}

const val MAX_BASE_STAT = 255
