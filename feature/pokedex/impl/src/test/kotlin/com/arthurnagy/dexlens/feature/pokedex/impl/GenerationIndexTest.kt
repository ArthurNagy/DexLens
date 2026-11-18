package com.arthurnagy.dexlens.feature.pokedex.impl

import com.arthurnagy.dexlens.core.model.GenerationStart
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.model.SpeciesSearchResult
import com.arthurnagy.dexlens.core.testing.nationalDexSummaries
import com.arthurnagy.dexlens.core.testing.nationalGenerationStarts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class GenerationIndexTest {
    private val fullList = nationalDexSummaries().map { SpeciesSearchResult(it, matchedName = null) }
    private val slots = generationSlots(fullList, nationalGenerationStarts())

    // Scenario: Dragging jumps to each generation's first species
    @Test
    fun everyGenerationStartsAtItsFirstSpecies() {
        assertEquals((1..9).toList(), slots.map { it.generation })
        assertEquals(listOf(0, 151, 251, 386, 493, 649, 721, 809, 905), slots.map { it.firstIndex })
    }

    // Scenario: Dragging jumps to each generation's first species
    @Test
    fun startsMissingFromTheListAreDropped() {
        val withoutJohto = fullList.filterNot { it.summary.id.value == 152 }
        assertEquals((1..9).filter { it != 2 }, generationSlots(withoutJohto, nationalGenerationStarts()).map { it.generation })
    }

    @Test
    fun fewerThanTwoSlotsGiveNone() {
        assertEquals(emptyList(), generationSlots(fullList.take(100), nationalGenerationStarts()))
        assertEquals(emptyList(), generationSlots(fullList, listOf(GenerationStart(1, SpeciesId(1)))))
        assertEquals(emptyList(), generationSlots(emptyList(), nationalGenerationStarts()))
    }

    // Scenario: Thumb follows hand scrolling
    @Test
    fun theTopRowPicksItsGeneration() {
        assertEquals(0, slotOfIndex(0, slots))
        assertEquals(0, slotOfIndex(150, slots))
        assertEquals(1, slotOfIndex(151, slots))
        assertEquals(4, slotOfIndex(493, slots))
        assertEquals(4, slotOfIndex(648, slots))
        assertEquals(5, slotOfIndex(649, slots))
        assertEquals(8, slotOfIndex(1024, slots))
    }

    @Test
    fun anIndexBeforeTheFirstSlotIsTheFirstSlot() {
        val late = listOf(GenerationSlot(3, 10), GenerationSlot(4, 20))
        assertEquals(0, slotOfIndex(2, late))
    }

    @Test
    fun theTrackSplitsIntoEqualClampedSlots() {
        assertEquals(0, slotAt(-50f, trackTop = 100f, trackHeight = 900f, count = 9))
        assertEquals(0, slotAt(100f, trackTop = 100f, trackHeight = 900f, count = 9))
        assertEquals(0, slotAt(199f, trackTop = 100f, trackHeight = 900f, count = 9))
        assertEquals(1, slotAt(200f, trackTop = 100f, trackHeight = 900f, count = 9))
        assertEquals(8, slotAt(999f, trackTop = 100f, trackHeight = 900f, count = 9))
        assertEquals(8, slotAt(5_000f, trackTop = 100f, trackHeight = 900f, count = 9))
    }

    @Test
    fun anEmptyTrackHasOnlyTheFirstSlot() {
        assertEquals(0, slotAt(300f, trackTop = 0f, trackHeight = 0f, count = 9))
        assertEquals(0, slotAt(300f, trackTop = 0f, trackHeight = 900f, count = 0))
    }

    @Test
    fun eachSlotCentreIsInItsSlot() {
        assertEquals(150f, slotCentre(0, trackTop = 100f, trackHeight = 900f, count = 9))
        assertEquals(950f, slotCentre(8, trackTop = 100f, trackHeight = 900f, count = 9))
        repeat(9) { slot -> assertEquals(slot, slotAt(slotCentre(slot, 37f, 512f, 9), 37f, 512f, 9)) }
    }

    @Test
    fun regionsInGenerationOrder() {
        val regions = listOf(
            R.string.pokedex_region_kanto,
            R.string.pokedex_region_johto,
            R.string.pokedex_region_hoenn,
            R.string.pokedex_region_sinnoh,
            R.string.pokedex_region_unova,
            R.string.pokedex_region_kalos,
            R.string.pokedex_region_alola,
            R.string.pokedex_region_galar,
            R.string.pokedex_region_paldea,
        )
        assertEquals(regions, (1..9).map { regionRes(it) })
        assertNull(regionRes(0))
        assertNull(regionRes(10))
    }
}
