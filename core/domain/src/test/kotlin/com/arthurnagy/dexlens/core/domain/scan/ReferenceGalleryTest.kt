package com.arthurnagy.dexlens.core.domain.scan

import com.arthurnagy.dexlens.core.domain.scan.ReferenceGallery.Companion.referenceCount
import com.arthurnagy.dexlens.core.domain.scan.ReferenceGallery.Companion.speciesAt
import com.arthurnagy.dexlens.core.model.SpeciesId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal class ReferenceGalleryTest {
    /** A reference that is 1 in dimension [axis] and 0 elsewhere, before quantising. */
    private data class Reference(
        val species: Int,
        val axis: Int,
        val value: Byte = SCALE.toInt().toByte(),
    )

    private fun file(
        references: List<Reference>,
        magic: String = "DXRF",
        version: Int = 1,
        dimension: Int = ReferenceGallery.DIMENSION,
        truncateBy: Int = 0,
    ): ByteArray {
        val count = references.size
        val idsEnd = 24 + 2 * count
        val padding = (4 - idsEnd % 4) % 4
        val buffer = ByteBuffer.allocate(idsEnd + padding + count * dimension).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(magic.toByteArray(Charsets.US_ASCII))
        buffer.putInt(version).putInt(count).putInt(dimension).putInt(1).putFloat(SCALE)
        references.forEach { buffer.putShort(it.species.toShort()) }
        repeat(padding) { buffer.put(0) }
        references.forEach { reference ->
            repeat(dimension) { d -> buffer.put(if (d == reference.axis) reference.value else 0) }
        }
        return buffer.array().copyOf(buffer.capacity() - truncateBy)
    }

    private fun unit(
        axis: Int,
        other: Int? = null,
    ): FloatArray = FloatArray(ReferenceGallery.DIMENSION).also {
        if (other == null) {
            it[axis] = 1f
        } else {
            it[axis] = SQRT_HALF
            it[other] = SQRT_HALF
        }
    }

    private val references = listOf(Reference(1, 0), Reference(4, 1), Reference(25, 2))

    @Test
    fun parsesIdsAndVectors() {
        val gallery = ReferenceGallery.parse(file(references))
        assertNotNull(gallery)
        gallery!!
        assertEquals(3, gallery.referenceCount)
        assertEquals(listOf(1, 4, 25), (0 until 3).map { gallery.speciesAt(it).value })
        assertEquals(1f, gallery.rank(unit(2)).score(SpeciesId(25)), TOLERANCE)
        assertEquals(0f, gallery.rank(unit(2)).score(SpeciesId(1)), TOLERANCE)
    }

    @Test
    fun anOddCountIsPaddedBeforeTheVectors() {
        val gallery = ReferenceGallery.parse(file(listOf(Reference(7, 3))))
        assertEquals(1f, gallery!!.rank(unit(3)).score(SpeciesId(7)), TOLERANCE)
    }

    @Test
    fun rejectsFilesItCannotUse() {
        assertNull(ReferenceGallery.parse(file(references, magic = "XXXX")))
        assertNull(ReferenceGallery.parse(file(references, version = 2)))
        assertNull(ReferenceGallery.parse(file(references, dimension = 128)))
        assertNull(ReferenceGallery.parse(file(references, truncateBy = 1)))
        assertNull(ReferenceGallery.parse(ByteArray(10)))
    }

    @Test
    fun rejectsSpeciesOutsideTheDex() {
        assertNull(ReferenceGallery.parse(file(listOf(Reference(0, 0)))))
        assertNull(ReferenceGallery.parse(file(listOf(Reference(1026, 0)))))
    }

    @Test
    fun keepsTheBestReferencePerSpecies() {
        val gallery = ReferenceGallery.parse(file(listOf(Reference(6, 0), Reference(6, 1, value = (SCALE / 2).toInt().toByte()))))
        assertEquals(1f, gallery!!.rank(unit(0)).score(SpeciesId(6)), TOLERANCE)
        assertEquals(63f / 127f, gallery.rank(unit(1)).score(SpeciesId(6)), TOLERANCE)
    }

    // Scenario: Form counts as its species
    @Test
    fun aFormsReferenceCountsAsItsSpecies() {
        // Charizard's base and Mega X pictures are both stored under #6.
        val gallery = ReferenceGallery.parse(file(listOf(Reference(6, 0), Reference(6, 1), Reference(150, 2))))
        val top = gallery!!.rank(unit(1)).top(1).single()
        assertEquals(SpeciesId(6), top.speciesId)
        assertEquals(1f, top.score, TOLERANCE)
    }

    @Test
    fun topRanksBestFirstAndScoreAnswersBelowIt() {
        val gallery = ReferenceGallery.parse(file(references))!!
        val scores = gallery.rank(unit(1, other = 2))
        assertEquals(listOf(4, 25), scores.top(2).map { it.speciesId.value })
        assertEquals(0f, scores.score(SpeciesId(1)), TOLERANCE)
        assertEquals(Float.NEGATIVE_INFINITY, scores.score(SpeciesId(150)))
    }

    private companion object {
        const val SCALE = 127f
        const val TOLERANCE = 0.001f
        val SQRT_HALF = kotlin.math.sqrt(0.5).toFloat()
    }
}
