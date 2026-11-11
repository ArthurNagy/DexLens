package com.arthurnagy.dexlens.core.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ArtworkLocationTest {
    private val location = ArtworkLocation("/content")

    private fun variety(
        id: Int,
        isDefault: Boolean,
    ) = Variety(id, "v$id", isDefault, 1, 1, listOf(PokemonType.FIRE), BaseStats(1, 1, 1, 1, 1, 1), emptyList(), emptyList())

    // Scenario: Species screens use the default variety
    @Test
    fun aSpeciesUsesItsDefaultVarietysPicture() {
        assertEquals("/content/artwork/6.webp", location.artwork(SpeciesId(6)))
        assertEquals(location.artwork(SpeciesId(6)), location.artwork(variety(6, isDefault = true)))
    }

    @Test
    fun anAlternateVarietyHasItsOwnPicture() {
        assertEquals("/content/artwork/10034.webp", location.artwork(variety(10034, isDefault = false)))
    }

    @Test
    fun nothingBeforeTheDownloadFinishes() {
        assertNull(ArtworkLocation(null).artwork(SpeciesId(6)))
        assertNull(ArtworkLocation(null).artwork(variety(10034, isDefault = false)))
    }
}
