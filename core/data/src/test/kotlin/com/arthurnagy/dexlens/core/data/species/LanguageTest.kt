package com.arthurnagy.dexlens.core.data.species

import org.junit.Test
import java.util.Locale
import kotlin.test.assertEquals

internal class LanguageTest {
    @Test
    fun mapsBundledLanguages() {
        assertEquals(ENGLISH, languageIdFor(Locale.ENGLISH))
        assertEquals(5, languageIdFor(Locale.FRENCH))
        assertEquals(6, languageIdFor(Locale.GERMAN))
        assertEquals(8, languageIdFor(Locale.ITALIAN))
        assertEquals(3, languageIdFor(Locale.KOREAN))
        assertEquals(11, languageIdFor(Locale.JAPANESE))
    }

    @Test
    fun mapsChineseScriptsAndSpanishRegions() {
        assertEquals(12, languageIdFor(Locale.forLanguageTag("zh-Hans-CN")))
        assertEquals(12, languageIdFor(Locale.forLanguageTag("zh-CN")))
        assertEquals(4, languageIdFor(Locale.forLanguageTag("zh-Hant")))
        assertEquals(4, languageIdFor(Locale.forLanguageTag("zh-TW")))
        assertEquals(7, languageIdFor(Locale.forLanguageTag("es-ES")))
        assertEquals(14, languageIdFor(Locale.forLanguageTag("es-MX")))
    }

    @Test
    fun unsupportedFallsBackToEnglish() {
        assertEquals(ENGLISH, languageIdFor(Locale.forLanguageTag("ro-RO")))
    }
}
