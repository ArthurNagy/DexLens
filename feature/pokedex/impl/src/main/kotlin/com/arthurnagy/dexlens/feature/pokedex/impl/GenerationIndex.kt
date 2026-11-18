package com.arthurnagy.dexlens.feature.pokedex.impl

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.arthurnagy.dexlens.core.model.GenerationStart
import com.arthurnagy.dexlens.core.model.SpeciesSearchResult
import kotlin.math.floor

/** A generation as the list shows it: [firstIndex] is the list position of its first species. */
@Immutable
internal data class GenerationSlot(
    val generation: Int,
    val firstIndex: Int,
)

/**
 * Where each generation starts in [listed]. A start whose species isn't listed is dropped, and fewer than two slots give
 * none at all, since a thumb with one stop has nowhere to go.
 */
internal fun generationSlots(
    listed: List<SpeciesSearchResult>,
    starts: List<GenerationStart>,
): List<GenerationSlot> {
    val indexById = HashMap<Int, Int>(listed.size)
    listed.forEachIndexed { index, result -> indexById[result.summary.id.value] = index }
    val slots = starts.mapNotNull { start -> indexById[start.firstSpecies.value]?.let { GenerationSlot(start.generation, it) } }
    return if (slots.size < MIN_SLOTS) emptyList() else slots
}

/** The slot of the generation holding list position [index]: the last one starting at or before it. */
internal fun slotOfIndex(
    index: Int,
    slots: List<GenerationSlot>,
): Int {
    var low = 0
    var high = slots.lastIndex
    while (low < high) {
        val mid = (low + high + 1) / 2
        if (slots[mid].firstIndex <= index) low = mid else high = mid - 1
    }
    return low
}

/** The slot under [y] on a track split into [count] equal slots, clamped to the track's ends. */
internal fun slotAt(
    y: Float,
    trackTop: Float,
    trackHeight: Float,
    count: Int,
): Int {
    if (trackHeight <= 0f || count <= 0) return 0
    return floor((y - trackTop) / (trackHeight / count)).toInt().coerceIn(0, count - 1)
}

/** The middle of [slot] on a track split into [count] equal slots. */
internal fun slotCentre(
    slot: Int,
    trackTop: Float,
    trackHeight: Float,
    count: Int,
): Float = trackTop + (slot + HALF) * trackHeight / count

/** The region a generation's games are set in, or null for a generation the app has no name for. */
@StringRes
internal fun regionRes(generation: Int): Int? = when (generation) {
    1 -> R.string.pokedex_region_kanto
    2 -> R.string.pokedex_region_johto
    3 -> R.string.pokedex_region_hoenn
    4 -> R.string.pokedex_region_sinnoh
    5 -> R.string.pokedex_region_unova
    6 -> R.string.pokedex_region_kalos
    7 -> R.string.pokedex_region_alola
    8 -> R.string.pokedex_region_galar
    9 -> R.string.pokedex_region_paldea
    else -> null
}

private const val MIN_SLOTS = 2
private const val HALF = 0.5f
