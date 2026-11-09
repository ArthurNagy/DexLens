package com.arthurnagy.dexlens.core.domain.species

import java.text.Normalizer
import java.util.Locale

/**
 * Turns a name or query into the tokens the search index stores. `tools/build_database.py` runs the same steps
 * on the bundled names, so the two must change together.
 */
internal object SpeciesNameNormalizer {
    /** "Mr. Mime" → `[mr, mime]`, "Nidoran♀" → `[nidoran, f]`, "Flabébé" → `[flabebe]`. */
    fun tokens(text: String): List<String> {
        val lowered = text.replace("♀", " f ").replace("♂", " m ").lowercase(Locale.ROOT)
        val decomposed = Normalizer.normalize(lowered, Normalizer.Form.NFD)
        val folded = StringBuilder(decomposed.length)
        for (char in decomposed) {
            val isMark = Character.getType(char) == Character.NON_SPACING_MARK.toInt()
            if (isMark && folded.isNotEmpty() && folded.last().isLatin()) continue
            folded.append(char)
        }
        val recomposed = Normalizer.normalize(folded, Normalizer.Form.NFC)
        return tokenPattern.findAll(recomposed).map { it.value }.toList()
    }

    /** Every token joined: the form used for whole-name comparison, for example `mrmime`. */
    fun compact(text: String): String = tokens(text).joinToString(separator = "")

    /** An FTS5 prefix query, or null when the text has nothing searchable. */
    fun toFtsPrefixQuery(text: String): String? = tokens(text).takeIf { it.isNotEmpty() }?.joinToString(" ") { "\"$it\"*" }

    private fun Char.isLatin(): Boolean = Character.UnicodeScript.of(code) == Character.UnicodeScript.LATIN

    private val tokenPattern = Regex("""[\p{L}\p{N}]+""")
}
