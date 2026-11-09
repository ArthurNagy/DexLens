package com.arthurnagy.dexlens.core.data.content

import com.arthurnagy.dexlens.core.data.species.DisplayLanguage
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.StringReader
import kotlin.test.assertEquals

internal class FlavorTextImporterTest {
    private val csv = listOf(
        "species_id,version_id,language_id,flavor_text",
        "1,1,9,\"A strange seed was\nplanted on its back.\"",
        "1,1,5,\"Bulbizarre a une étrange graine plantée sur son dos.\"",
        "25,2,1,\"Skipped\"",
        "25,2,9,\"It says \"\"pika\"\"\"",
        "",
    ).joinToString("\n")

    // Scenario: Flavor text is imported
    @Test
    fun importsWantedLanguagesWithCleanText() = runTest {
        val dao = FakeContentDao()
        FlavorTextImporter(dao, DisplayLanguage { 5 }).import(csv.byteInputStream())

        assertEquals(setOf(9, 5), dao.rows.map { it.languageId }.toSet())
        assertEquals("A strange seed was planted on its back.", dao.flavorText(1, 9).single().text)
        assertEquals("It says \"pika\"", dao.flavorText(25, 9).single().text)
    }

    @Test
    fun cleansGameLineBreaks() {
        assertEquals("POKé-mon hides here", cleanFlavorText("POKé-\u00AD\nmon\u000Chides\nhere"))
        assertEquals("one two", cleanFlavorText("one\n\ntwo"))
    }

    @Test
    fun parsesQuotesAndLastLineWithoutNewline() = runTest {
        val rows = mutableListOf<List<String>>()
        forEachCsvRow(StringReader("a,b\n1,\"x,y\"\n2,z").buffered()) { rows += it }
        assertEquals(listOf(listOf("1", "x,y"), listOf("2", "z")), rows)
    }
}
