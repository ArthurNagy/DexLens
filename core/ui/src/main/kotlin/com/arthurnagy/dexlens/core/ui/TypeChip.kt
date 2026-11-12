package com.arthurnagy.dexlens.core.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.arthurnagy.dexlens.core.designsystem.AppDimens
import com.arthurnagy.dexlens.core.designsystem.AppSpacing
import com.arthurnagy.dexlens.core.designsystem.color
import com.arthurnagy.dexlens.core.designsystem.onColor
import com.arthurnagy.dexlens.core.model.PokemonType

@StringRes
fun PokemonType.labelRes(): Int = when (this) {
    PokemonType.NORMAL -> R.string.core_ui_type_normal
    PokemonType.FIGHTING -> R.string.core_ui_type_fighting
    PokemonType.FLYING -> R.string.core_ui_type_flying
    PokemonType.POISON -> R.string.core_ui_type_poison
    PokemonType.GROUND -> R.string.core_ui_type_ground
    PokemonType.ROCK -> R.string.core_ui_type_rock
    PokemonType.BUG -> R.string.core_ui_type_bug
    PokemonType.GHOST -> R.string.core_ui_type_ghost
    PokemonType.STEEL -> R.string.core_ui_type_steel
    PokemonType.FIRE -> R.string.core_ui_type_fire
    PokemonType.WATER -> R.string.core_ui_type_water
    PokemonType.GRASS -> R.string.core_ui_type_grass
    PokemonType.ELECTRIC -> R.string.core_ui_type_electric
    PokemonType.PSYCHIC -> R.string.core_ui_type_psychic
    PokemonType.ICE -> R.string.core_ui_type_ice
    PokemonType.DRAGON -> R.string.core_ui_type_dragon
    PokemonType.DARK -> R.string.core_ui_type_dark
    PokemonType.FAIRY -> R.string.core_ui_type_fairy
}

/** A pill in the type's colour, with its name as text, so colour never carries the meaning alone. */
@Composable
fun TypeChip(
    type: PokemonType,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
) {
    val label = stringResource(type.labelRes())
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = AppDimens.typeChipHeight)
            // A shaped background draws the pill without the graphics layer a clip would add to every chip.
            .background(type.color, CircleShape)
            .padding(horizontal = AppSpacing.sm + AppSpacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (trailingText == null) label else stringResource(R.string.core_ui_type_with_detail, label, trailingText),
            style = MaterialTheme.typography.labelMedium,
            color = type.onColor,
        )
    }
}

@Composable
fun TypeChipRow(
    types: List<PokemonType>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
    ) {
        types.forEach { TypeChip(it) }
    }
}
