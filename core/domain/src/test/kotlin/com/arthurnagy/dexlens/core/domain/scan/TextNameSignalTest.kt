package com.arthurnagy.dexlens.core.domain.scan

import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.model.SpeciesNameHit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class TextNameSignalTest {
    private fun hit(
        id: Int,
        name: String,
    ) = SpeciesNameHit(SpeciesId(id), name)

    private val names = listOf(
        hit(1, "Bulbasaur"),
        hit(1, "Bulbizarre"),
        hit(1, "フシギダネ"),
        hit(16, "Pidgey"),
        hit(25, "Pikachu"),
        hit(29, "Nidoran♀"),
        hit(32, "Nidoran♂"),
        hit(63, "Abra"),
        hit(83, "Farfetch’d"),
        hit(89, "Muk"),
        hit(122, "Mr. Mime"),
        hit(143, "Snorlax"),
        hit(151, "Mew"),
        hit(772, "Type: Null"),
    )
    private val signal = TextNameSignal(ScanNameIndex(names))

    private fun ids(vararg lines: String) = signal.analyze(FrameObservation(lines.toList())).candidates.map { it.speciesId.value }.toSet()

    // Scenario: Uppercase game name
    @Test
    fun uppercaseName() = assertEquals(setOf(25), ids("PIKACHU"))

    // Scenario: Punctuation variants match
    @Test
    fun punctuationVariants() {
        assertEquals(setOf(122), ids("MR.MIME"))
        assertEquals(setOf(122), ids("Mr. Mime"))
        assertEquals(setOf(83), ids("FARFETCH'D"))
        assertEquals(setOf(772), ids("Type: Null"))
    }

    // Scenario: Gender symbol selects the species
    @Test
    fun genderSymbolSelects() {
        assertEquals(setOf(29), ids("NIDORAN♀"))
        assertEquals(setOf(32), ids("NIDORAN♂"))
    }

    // Scenario: Missing gender symbol offers both
    @Test
    fun missingGenderSymbolOffersBoth() {
        assertEquals(setOf(29, 32), ids("NIDORAN"))
        assertEquals(setOf(29, 32), ids("NIDORAN?"))
        assertEquals(setOf(29, 32), ids("NIDORANQ"))
    }

    // Scenario: Name inside a sentence
    @Test
    fun nameInsideSentence() {
        assertEquals(setOf(16), ids("Wild PIDGEY appeared!"))
        assertEquals(setOf(25, 16), ids("Go! PIKACHU!", "Foe PIDGEY"))
    }

    // Scenario: Other Latin-script language
    @Test
    fun otherLatinLanguage() = assertEquals(setOf(1), ids("BULBIZARRE"))

    // Scenario: Digit misread as a letter
    @Test
    fun digitMisreads() {
        assertEquals(setOf(25), ids("P1KACHU"))
        assertEquals(setOf(143), ids("5NORLAX"))
        assertEquals(setOf(143), ids("SN0RLAX"))
    }

    // Scenario: One wrong letter in a long name
    @Test
    fun oneWrongLetter() {
        assertEquals(setOf(1), ids("BULBASAUB"))
        val candidate = signal.analyze(FrameObservation(listOf("BULBASAUB"))).candidates.single()
        assertEquals(MatchKind.FUZZY, candidate.kind)
    }

    // Scenario: Short names stay strict
    @Test
    fun shortNamesStayStrict() {
        assertTrue(ids("New").isEmpty())
        assertTrue(ids("Mud").isEmpty())
        assertTrue(ids("Abro").isEmpty())
        assertEquals(setOf(151), ids("MEW"))
        assertEquals(setOf(89), ids("MUK"))
    }

    // Scenario: Ordinary game words ignored
    @Test
    fun ordinaryWordsIgnored() {
        assertTrue(ids("FIGHT BAG POKéMON RUN").isEmpty())
        assertTrue(ids("What will you do?").isEmpty())
        assertTrue(ids("").isEmpty())
    }

    @Test
    fun ambiguousFuzzyMatchIsNoCandidate() {
        val tied = TextNameSignal(ScanNameIndex(listOf(hit(1, "Charmander"), hit(2, "Charmenber"))))
        val result = tied.analyze(FrameObservation(listOf("CHARMANBER")))
        assertTrue(result.candidates.isEmpty())
        assertEquals(MatchOutcome.AMBIGUOUS, result.decisions.single().outcome)
    }

    @Test
    fun decisionsExplainOutcomes() {
        val result = signal.analyze(FrameObservation(listOf("PIKACHU", "P1KACHU", "NICKNAME", "FIGHT")))
        assertEquals(
            listOf(MatchOutcome.EXACT, MatchOutcome.CONFUSABLE, MatchOutcome.NO_MATCH, MatchOutcome.IGNORED),
            result.decisions.map { it.outcome },
        )
    }

    @Test
    fun wordsAreNotReused() {
        val overlapping = TextNameSignal(ScanNameIndex(listOf(hit(122, "Mr. Mime"), hit(9, "Mime"))))
        assertEquals(listOf(122), overlapping.analyze(FrameObservation(listOf("Mr. Mime"))).candidates.map { it.speciesId.value })
    }

    @Test
    fun allowedEditsScaleWithLength() {
        assertEquals(0, ScanNameIndex.allowedEdits(4))
        assertEquals(0, ScanNameIndex.allowedEdits(7))
        assertEquals(1, ScanNameIndex.allowedEdits(8))
        assertEquals(1, ScanNameIndex.allowedEdits(9))
        assertEquals(2, ScanNameIndex.allowedEdits(10))
    }

    @Test
    fun boundedDistanceStopsEarly() {
        assertEquals(2, ScanNameIndex.boundedEditDistance("abcdef", "abcxyf", 3))
        assertEquals(2, ScanNameIndex.boundedEditDistance("abc", "xyz", 1))
    }
}
