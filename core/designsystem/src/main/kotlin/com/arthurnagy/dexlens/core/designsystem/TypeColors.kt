package com.arthurnagy.dexlens.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.arthurnagy.dexlens.core.model.PokemonType

/** The app's own colour for each battle type. */
val PokemonType.color: Color
    get() = when (this) {
        PokemonType.NORMAL -> Color(0xFF9A9A8E)
        PokemonType.FIRE -> Color(0xFFE5722E)
        PokemonType.WATER -> Color(0xFF3C86DA)
        PokemonType.ELECTRIC -> Color(0xFFF1BE2A)
        PokemonType.GRASS -> Color(0xFF4CA63B)
        PokemonType.ICE -> Color(0xFF55C4DA)
        PokemonType.FIGHTING -> Color(0xFFC0413C)
        PokemonType.POISON -> Color(0xFF9247C4)
        PokemonType.GROUND -> Color(0xFFB98A4B)
        PokemonType.FLYING -> Color(0xFF85A9EA)
        PokemonType.PSYCHIC -> Color(0xFFE85E96)
        PokemonType.BUG -> Color(0xFF93A62A)
        PokemonType.ROCK -> Color(0xFFA9995B)
        PokemonType.GHOST -> Color(0xFF6A5595)
        PokemonType.DRAGON -> Color(0xFF5A63D8)
        PokemonType.DARK -> Color(0xFF5B4E48)
        PokemonType.STEEL -> Color(0xFF7DA3B5)
        PokemonType.FAIRY -> Color(0xFFE48DD3)
    }

/** Black or white, whichever reads better on this type's colour. */
val PokemonType.onColor: Color
    get() = if (color.luminance() > ON_COLOR_LUMINANCE_THRESHOLD) Color(0xFF14181A) else Color.White

/** The type colour faded into the surface, for large areas such as the entry header. */
internal fun ColorScheme.typeWash(type: PokemonType): Color = type.color.copy(alpha = TYPE_WASH_ALPHA).compositeOver(surface)

/** The entry header's two gradient stops: primary type, then secondary or a softer primary. */
@Composable
@ReadOnlyComposable
fun typeGradient(types: List<PokemonType>): List<Color> {
    val first = types.first()
    return listOf(MaterialTheme.colorScheme.typeWash(first), MaterialTheme.colorScheme.typeWash(types.getOrElse(1) { first }))
}

/** The luminance where black and white text have equal contrast, so each type gets at least 4.5:1. */
private const val ON_COLOR_LUMINANCE_THRESHOLD = 0.179f
private const val TYPE_WASH_ALPHA = 0.45f
