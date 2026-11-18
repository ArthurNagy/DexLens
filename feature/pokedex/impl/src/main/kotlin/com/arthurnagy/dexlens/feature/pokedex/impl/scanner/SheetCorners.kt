package com.arthurnagy.dexlens.feature.pokedex.impl.scanner

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * The larger top corner radius, in pixels, of [shape] drawn at [size]: what the sheet's rounded corners leave uncovered
 * above it. Measured from the outline the shape actually draws, so it follows the theme; 0 for a shape without rounded
 * corners.
 */
internal fun topCornerRadius(
    shape: Shape,
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density,
): Float {
    val outline = shape.createOutline(size, layoutDirection, density) as? Outline.Rounded ?: return 0f
    return maxOf(outline.roundRect.topLeftCornerRadius.y, outline.roundRect.topRightCornerRadius.y)
}
