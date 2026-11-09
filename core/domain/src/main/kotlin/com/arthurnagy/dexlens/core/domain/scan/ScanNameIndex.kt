package com.arthurnagy.dexlens.core.domain.scan

import com.arthurnagy.dexlens.core.domain.species.SpeciesNameNormalizer
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.model.SpeciesNameHit

/**
 * Species names in Latin script, keyed by their compact form, in the forms OCR can compare against.
 *
 * A name with a gender symbol is also reachable without it, because OCR often drops the symbol: `nidoran` maps to
 * both Nidoran♀ and Nidoran♂.
 */
class ScanNameIndex(
    names: List<SpeciesNameHit>,
) {
    private val exact = HashMap<String, MutableSet<SpeciesId>>()
    private val folded = HashMap<String, MutableSet<SpeciesId>>()

    /** Folded keys grouped by length, for bounded edit distance. */
    private val foldedByLength: Map<Int, List<Map.Entry<String, Set<SpeciesId>>>>

    /** Stems of gendered names, such as `nidoran`. */
    private val genderedStems = HashSet<String>()

    init {
        names.filter { isLatinName(it.name) }.forEach { hit ->
            val key = SpeciesNameNormalizer.compact(hit.name)
            if (key.length >= MIN_KEY_LENGTH) {
                add(key, hit.speciesId)
                if (hit.name.contains('♀') || hit.name.contains('♂')) {
                    val stem = key.dropLast(1)
                    genderedStems += stem
                    add(stem, hit.speciesId)
                }
            }
        }
        foldedByLength = folded.entries.groupBy({ it.key.length }, { it.toImmutableEntry() })
    }

    private fun add(
        key: String,
        id: SpeciesId,
    ) {
        exact.getOrPut(key) { LinkedHashSet() } += id
        folded.getOrPut(fold(key)) { LinkedHashSet() } += id
    }

    internal fun exact(key: String): Set<SpeciesId> = exact[key].orEmpty()

    internal fun confusable(key: String): Set<SpeciesId> = folded[fold(key)].orEmpty()

    /** Both species of a gendered family when [key] is its stem plus at most one misread symbol. */
    internal fun genderedFamily(key: String): Set<SpeciesId> {
        val stem = genderedStems.firstOrNull { key.startsWith(it) && key.length - it.length <= 1 } ?: return emptySet()
        return exact(stem)
    }

    /**
     * The species at the smallest edit distance within the allowance for the name's length. Several species at
     * that distance is a tie, and the caller treats it as no match rather than guessing.
     */
    internal fun nearest(key: String): FuzzyMatch? {
        val query = fold(key)
        var bestDistance = Int.MAX_VALUE
        val best = LinkedHashSet<SpeciesId>()
        for (length in query.length - MAX_EDITS..query.length + MAX_EDITS) {
            for ((candidate, ids) in foldedByLength[length].orEmpty()) {
                if (candidate.first() != query.first()) continue
                val allowed = allowedEdits(candidate.length)
                if (allowed == 0 || kotlin.math.abs(candidate.length - query.length) > allowed) continue
                val distance = boundedEditDistance(query, candidate, allowed)
                when {
                    distance > allowed -> {
                        Unit
                    }

                    distance < bestDistance -> {
                        bestDistance = distance
                        best.clear()
                        best += ids
                    }

                    distance == bestDistance -> {
                        best += ids
                    }
                }
            }
        }
        return if (best.isEmpty()) null else FuzzyMatch(best, bestDistance)
    }

    internal data class FuzzyMatch(
        val speciesIds: Set<SpeciesId>,
        val distance: Int,
    )

    companion object {
        internal const val MIN_KEY_LENGTH = 3
        private const val MAX_EDITS = 2
        private const val FOLDS_ONLY_BELOW = 8
        private const val TWO_EDITS_FROM = 10

        /**
         * Names under 8 letters allow no edits beyond character folding: too many ordinary words sit one letter away
         * ("golden" and Goldeen, "magma" and Magmar). A wrong lock-on is worse than a slow one.
         */
        internal fun allowedEdits(nameLength: Int): Int = when {
            nameLength < FOLDS_ONLY_BELOW -> 0
            nameLength < TWO_EDITS_FROM -> 1
            else -> MAX_EDITS
        }

        /** Maps characters OCR commonly confuses onto one letter: 0/O, 1/I/L, 5/S, 8/B, 2/Z and 6/G. */
        internal fun fold(key: String): String = buildString(key.length) {
            for (char in key) {
                append(
                    when (char) {
                        '0' -> 'o'
                        '1', 'i' -> 'l'
                        '5' -> 's'
                        '8' -> 'b'
                        '2' -> 'z'
                        '6' -> 'g'
                        else -> char
                    },
                )
            }
        }

        private fun isLatinName(name: String): Boolean = name.filter(Char::isLetter).let { letters ->
            letters.isNotEmpty() && letters.all { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.LATIN }
        }

        /** Levenshtein distance, stopping early once it exceeds [limit]. */
        internal fun boundedEditDistance(
            a: String,
            b: String,
            limit: Int,
        ): Int {
            var previous = IntArray(b.length + 1) { it }
            var current = IntArray(b.length + 1)
            for (i in 1..a.length) {
                current[0] = i
                var rowBest = current[0]
                for (j in 1..b.length) {
                    val substitution = previous[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1
                    current[j] = minOf(substitution, previous[j] + 1, current[j - 1] + 1)
                    rowBest = minOf(rowBest, current[j])
                }
                if (rowBest > limit) return limit + 1
                val swap = previous
                previous = current
                current = swap
            }
            return previous[b.length]
        }

        private fun <K, V> Map.Entry<K, V>.toImmutableEntry(): Map.Entry<K, V> = java.util.AbstractMap.SimpleImmutableEntry(key, value)
    }
}
