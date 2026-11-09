package com.arthurnagy.dexlens.core.data.species

import java.util.Locale
import javax.inject.Inject

/** The bundled database's id for the language names are displayed in. */
internal fun interface DisplayLanguage {
    fun languageId(): Int
}

internal class DeviceDisplayLanguage
    @Inject
    constructor() : DisplayLanguage {
        override fun languageId(): Int = languageIdFor(Locale.getDefault())
    }

/** Maps a locale to its bundled language, falling back to English when species names aren't bundled for it. */
internal fun languageIdFor(locale: Locale): Int = when (locale.language) {
    "ja" -> JAPANESE
    "ko" -> KOREAN
    "fr" -> FRENCH
    "de" -> GERMAN
    "it" -> ITALIAN
    "es" -> if (locale.country.isEmpty() || locale.country == "ES") SPANISH_SPAIN else SPANISH_LATIN_AMERICA
    "zh" -> if (locale.script == "Hant" || locale.country in TRADITIONAL_REGIONS) CHINESE_TRADITIONAL else CHINESE_SIMPLIFIED
    else -> ENGLISH
}

internal const val ENGLISH = 9
private const val KOREAN = 3
private const val CHINESE_TRADITIONAL = 4
private const val FRENCH = 5
private const val GERMAN = 6
private const val SPANISH_SPAIN = 7
private const val ITALIAN = 8
private const val JAPANESE = 11
private const val CHINESE_SIMPLIFIED = 12
private const val SPANISH_LATIN_AMERICA = 14
private val TRADITIONAL_REGIONS = setOf("TW", "HK", "MO")
