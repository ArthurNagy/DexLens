package com.arthurnagy.dexlens.core.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign

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
        Box(
            Modifier
                .weight(1f)
                .height(AppDimens.statBarHeight)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

const val MAX_BASE_STAT = 255
