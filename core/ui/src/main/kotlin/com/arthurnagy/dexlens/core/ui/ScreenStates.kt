package com.arthurnagy.dexlens.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import com.arthurnagy.dexlens.core.designsystem.AppDimens
import com.arthurnagy.dexlens.core.designsystem.AppSpacing
import com.arthurnagy.dexlens.core.designsystem.SkeletonShimmer

/** A number in the Pokédex format. */
@Composable
fun speciesNumberLabel(number: Int): String = stringResource(R.string.core_ui_species_number, number)

/** Joins words the way the app's language separates a list. */
@Composable
fun listText(items: List<String>): String = items.joinToString(stringResource(R.string.core_ui_list_separator))

/** Shimmers while a page loads. [content] draws the page's shape in skeleton blocks; screen readers hear one "Loading". */
@Composable
fun LoadingSkeleton(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val description = stringResource(R.string.core_ui_loading)
    SkeletonShimmer(modifier = modifier.fillMaxSize().clearAndSetSemantics { contentDescription = description }, content = content)
}

/** A centred message for empty and error states, with an optional action. */
@Composable
fun MessageState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(AppSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(AppDimens.iconLg + AppDimens.iconSm),
            )
        }
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (message != null) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null && onAction != null) {
            FilledTonalButton(onClick = onAction, modifier = Modifier.padding(top = AppSpacing.sm)) { Text(actionLabel) }
        }
    }
}
