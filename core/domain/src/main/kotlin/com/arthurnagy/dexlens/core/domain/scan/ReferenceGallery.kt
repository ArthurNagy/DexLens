package com.arthurnagy.dexlens.core.domain.scan

import com.arthurnagy.dexlens.core.model.SpeciesId

/**
 * The bundled reference pictures as 8-bit vectors, one or more per species. A frame's vector is compared with every
 * reference, and each species keeps its best match.
 *
 * File layout, little-endian: magic `DXRF`, version, count N, dimension D, bytes per value, scale, N uint16 National Dex
 * numbers, padding to 4 bytes, then N × D int8 values.
 */
class ReferenceGallery private constructor(
    private val species: ShortArray,
    private val vectors: ByteArray,
    private val scale: Float,
) {
    /** Scores every species against [embedding], a unit vector of [DIMENSION] numbers. */
    fun rank(embedding: FloatArray): PictureScores {
        require(embedding.size == DIMENSION) { "expected $DIMENSION numbers, got ${embedding.size}" }
        val best = FloatArray(PictureScores.SPECIES_SLOTS) { Float.NEGATIVE_INFINITY }
        var offset = 0
        for (reference in species.indices) {
            var sum = 0f
            for (d in 0 until DIMENSION) sum += embedding[d] * vectors[offset + d]
            offset += DIMENSION
            val id = species[reference].toInt() and UINT16_MASK
            if (sum > best[id]) best[id] = sum
        }
        for (id in best.indices) {
            if (best[id] > Float.NEGATIVE_INFINITY) best[id] /= scale
        }
        return PictureScores(best)
    }

    companion object {
        /** Numbers per vector, the encoder's output size. */
        internal const val DIMENSION = 256

        /** Reads a reference file, or returns null if it isn't a version 1 file this app can use. */
        @Suppress("ReturnCount")
        fun parse(bytes: ByteArray): ReferenceGallery? {
            if (bytes.size < HEADER_SIZE) return null
            if (!bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) return null
            val version = bytes.intAt(VERSION_OFFSET)
            val count = bytes.intAt(COUNT_OFFSET)
            val dimension = bytes.intAt(DIMENSION_OFFSET)
            val bytesPerValue = bytes.intAt(BYTES_PER_VALUE_OFFSET)
            val scale = Float.fromBits(bytes.intAt(SCALE_OFFSET))
            if (version != VERSION || dimension != DIMENSION || bytesPerValue != 1 || count <= 0 || !(scale > 0f)) return null
            val idsEnd = HEADER_SIZE + 2L * count
            val vectorsStart = idsEnd + (ALIGNMENT - idsEnd % ALIGNMENT) % ALIGNMENT
            if (bytes.size.toLong() != vectorsStart + count.toLong() * dimension) return null
            val species = ShortArray(count) { bytes.uint16At(HEADER_SIZE + 2 * it).toShort() }
            if (species.any { (it.toInt() and UINT16_MASK) !in 1..MAX_SPECIES_ID }) return null
            val vectors = bytes.copyOfRange(vectorsStart.toInt(), bytes.size)
            return ReferenceGallery(species, vectors, scale)
        }

        /** The species a reference belongs to, for tests. */
        internal fun ReferenceGallery.speciesAt(index: Int): SpeciesId = SpeciesId(species[index].toInt() and UINT16_MASK)

        internal val ReferenceGallery.referenceCount: Int get() = species.size

        /** A little-endian 32-bit integer. */
        private fun ByteArray.intAt(offset: Int): Int = (0 until Int.SIZE_BYTES).fold(0) { value, i ->
            value or ((this[offset + i].toInt() and BYTE_MASK) shl (Byte.SIZE_BITS * i))
        }

        /** A little-endian unsigned 16-bit integer. */
        private fun ByteArray.uint16At(offset: Int): Int =
            (this[offset].toInt() and BYTE_MASK) or ((this[offset + 1].toInt() and BYTE_MASK) shl Byte.SIZE_BITS)

        private val MAGIC = "DXRF".toByteArray(Charsets.US_ASCII)
        private const val VERSION_OFFSET = 4
        private const val COUNT_OFFSET = 8
        private const val DIMENSION_OFFSET = 12
        private const val BYTES_PER_VALUE_OFFSET = 16
        private const val SCALE_OFFSET = 20
        private const val BYTE_MASK = 0xFF
        private const val VERSION = 1
        private const val HEADER_SIZE = 24
        private const val ALIGNMENT = 4
        private const val UINT16_MASK = 0xFFFF
    }
}
