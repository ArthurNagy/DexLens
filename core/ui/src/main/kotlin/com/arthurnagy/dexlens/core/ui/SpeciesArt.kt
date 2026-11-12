package com.arthurnagy.dexlens.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.size.Size
import com.arthurnagy.dexlens.core.designsystem.AppDimens
import com.arthurnagy.dexlens.core.designsystem.color
import com.arthurnagy.dexlens.core.designsystem.onColor
import com.arthurnagy.dexlens.core.model.PokemonType
import java.io.File

/**
 * A species' picture slot: a tile tinted with its types, with the downloaded art on top when [imagePath] is set.
 * Coil only ever receives local files. When [imagePath] cannot be loaded, [fallbackPath] is tried instead. Without art
 * the tile shows the name's first letter in a lens motif.
 */
@Composable
fun SpeciesArt(
    name: String,
    types: List<PokemonType>,
    imagePath: String?,
    contentDescription: String?,
    letterStyle: TextStyle,
    modifier: Modifier = Modifier,
    fallbackPath: String? = null,
) {
    val first = types.first()
    val second = types.getOrElse(1) { first }
    val brush = remember(first, second) { Brush.linearGradient(listOf(first.color, second.color.copy(alpha = TILE_END_ALPHA))) }
    val shape = remember { RoundedCornerShape(AppDimens.artCorner) }
    Box(
        modifier = modifier.clip(shape).background(brush),
        contentAlignment = Alignment.Center,
    ) {
        // Always the stored size, so every place a species appears shares one decoded image and the hero never waits for it.
        val context = LocalPlatformContext.current
        var primaryFailed by remember(imagePath) { mutableStateOf(false) }
        val path = if (primaryFailed && fallbackPath != null) fallbackPath else imagePath
        val request = remember(context, path) {
            path?.let { ImageRequest.Builder(context).data(File(it)).size(Size.ORIGINAL).build() }
        }
        val painter = rememberAsyncImagePainter(
            model = request,
            // Only the current primary picture's failure switches to the fallback, never a stale one from before a change.
            onError = { error -> if (imagePath != null && error.result.request.data == File(imagePath)) primaryFailed = true },
        )
        val painterState by painter.state.collectAsState()
        val loaded = painterState is AsyncImagePainter.State.Success
        if (!loaded) {
            LensMotif(color = first.onColor, modifier = Modifier.fillMaxSize())
            Text(text = name.take(1).uppercase(), color = first.onColor.copy(alpha = LETTER_ALPHA), style = letterStyle)
        }
        Image(
            painter = painter,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun LensMotif(
    color: Color,
    modifier: Modifier = Modifier,
) {
    // The stroke is built once per size, not on every draw of every placeholder in the list.
    Spacer(
        modifier.drawWithCache {
            val base = size.minDimension
            val stroke = Stroke(width = base * RING_WIDTH)
            val ring = color.copy(alpha = RING_ALPHA)
            onDrawBehind {
                drawCircle(ring, radius = base * OUTER_RING, style = stroke)
                drawCircle(ring, radius = base * INNER_RING, style = stroke)
            }
        },
    )
}

private const val TILE_END_ALPHA = 0.72f
private const val LETTER_ALPHA = 0.92f
private const val RING_ALPHA = 0.16f
private const val OUTER_RING = 0.44f
private const val INNER_RING = 0.30f
private const val RING_WIDTH = 0.035f
