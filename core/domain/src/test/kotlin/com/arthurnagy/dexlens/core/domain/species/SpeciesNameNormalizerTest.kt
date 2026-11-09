package com.arthurnagy.dexlens.core.domain.species

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

internal class SpeciesNameNormalizerTest {
    private fun assertTokens(
        expected: List<String>,
        text: String,
    ) = assertEquals(text, expected, SpeciesNameNormalizer.tokens(text))

    @Test
    fun trickyNames() {
        assertTokens(listOf("mr", "mime"), "Mr. Mime")
        assertTokens(listOf("nidoran", "f"), "Nidoran♀")
        assertTokens(listOf("nidoran", "m"), "Nidoran♂")
        assertTokens(listOf("farfetch", "d"), "Farfetch’d")
        assertTokens(listOf("farfetch", "d"), "FARFETCH'D")
        assertTokens(listOf("flabebe"), "Flabébé")
        assertTokens(listOf("type", "null"), "Type: Null")
        assertTokens(listOf("ho", "oh"), "Ho-Oh")
        assertTokens(listOf("porygon", "z"), "Porygon-Z")
    }

    @Test
    fun keepsJapaneseVoicingMarks() {
        assertEquals(listOf("ガーディ"), SpeciesNameNormalizer.tokens("ガーディ"))
        assertEquals(listOf("フシギダネ"), SpeciesNameNormalizer.tokens("フシギダネ"))
    }

    @Test
    fun compactJoinsTokens() {
        assertEquals("mrmime", SpeciesNameNormalizer.compact("MR.MIME"))
        assertEquals("nidoranf", SpeciesNameNormalizer.compact("NIDORAN♀"))
    }

    @Test
    fun buildsQuotedPrefixQuery() {
        assertEquals("\"mr\"* \"mime\"*", SpeciesNameNormalizer.toFtsPrefixQuery("mr mime"))
        assertEquals("\"flabebe\"*", SpeciesNameNormalizer.toFtsPrefixQuery("Flabébé"))
        assertNull(SpeciesNameNormalizer.toFtsPrefixQuery(" - . "))
    }
}
