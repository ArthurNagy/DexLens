package com.arthurnagy.dexlens.core.data.content

import com.arthurnagy.dexlens.core.data.species.DisplayLanguage
import com.arthurnagy.dexlens.core.data.species.ENGLISH
import com.arthurnagy.dexlens.core.database.ContentDao
import com.arthurnagy.dexlens.core.database.FlavorTextEntity
import java.io.BufferedReader
import java.io.InputStream
import javax.inject.Inject

/** Stores PokeAPI's flavor text table, for English and the display language only, cleaned of the games' line breaks. */
internal class FlavorTextImporter
    @Inject
    constructor(
        private val dao: ContentDao,
        private val displayLanguage: DisplayLanguage,
    ) {
        /** Reads the whole table first and replaces the stored text in one transaction, so a failed read changes nothing. */
        suspend fun import(stream: InputStream) {
            val wanted = setOf(ENGLISH, displayLanguage.languageId())
            val rows = ArrayList<FlavorTextEntity>()
            forEachCsvRow(stream.bufferedReader(Charsets.UTF_8)) { row ->
                val language = row.getOrNull(LANGUAGE)?.toIntOrNull()
                if (language != null && language in wanted) {
                    rows += FlavorTextEntity(row[SPECIES].toInt(), row[VERSION].toInt(), language, cleanFlavorText(row[TEXT]))
                }
            }
            dao.replaceAll(rows)
        }

        private companion object {
            const val SPECIES = 0
            const val VERSION = 1
            const val LANGUAGE = 2
            const val TEXT = 3
        }
    }

/** Games wrap text with form feeds, line breaks and soft hyphens. Soft hyphens at a break join the word. */
internal fun cleanFlavorText(raw: String): String = raw
    .replace(Regex("""­[\n\f]"""), "")
    .replace("­", "")
    .replace(Regex("""[\f\n\r]+"""), " ")
    .replace(Regex(""" {2,}"""), " ")
    .trim()

/** Calls [onRow] for each data row of an RFC 4180 CSV, whose quoted fields may contain line breaks. Skips the header. */
internal suspend fun forEachCsvRow(
    reader: BufferedReader,
    onRow: suspend (List<String>) -> Unit,
) {
    var isHeader = true
    val fields = ArrayList<String>()
    val field = StringBuilder()
    var inQuotes = false
    var value = reader.read()
    while (value != -1) {
        val char = value.toChar()
        when {
            inQuotes && char == '"' -> {
                val next = reader.read()
                if (next == '"'.code) {
                    field.append('"')
                } else {
                    inQuotes = false
                    value = next
                    continue
                }
            }

            inQuotes -> {
                field.append(char)
            }

            char == '"' -> {
                inQuotes = true
            }

            char == ',' -> {
                fields += field.toString()
                field.clear()
            }

            char == '\n' -> {
                fields += field.toString()
                field.clear()
                if (!isHeader) onRow(fields.toList())
                isHeader = false
                fields.clear()
            }

            char != '\r' -> {
                field.append(char)
            }
        }
        value = reader.read()
    }
    if (field.isNotEmpty() || fields.isNotEmpty()) {
        fields += field.toString()
        if (!isHeader) onRow(fields.toList())
    }
}
