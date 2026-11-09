package com.arthurnagy.dexlens.core.data.scan

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arthurnagy.dexlens.core.database.DexDatabase
import com.arthurnagy.dexlens.core.domain.scan.FrameObservation
import com.arthurnagy.dexlens.core.domain.scan.MatchKind
import com.arthurnagy.dexlens.core.domain.scan.ScanNameIndex
import com.arthurnagy.dexlens.core.domain.scan.TextNameSignal
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.model.SpeciesNameHit
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Runs the matcher against every real bundled name and against text a game screen actually shows. */
@RunWith(AndroidJUnit4::class)
internal class ScanCorpusTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private lateinit var database: DexDatabase
    private lateinit var signal: TextNameSignal
    private lateinit var english: Map<Int, String>

    @Before
    fun open() = runTest {
        context.deleteDatabase(NAME)
        database = Room
            .databaseBuilder<DexDatabase>(context, NAME)
            .createFromAsset("databases/dexlens.db")
            .setDriver(BundledSQLiteDriver())
            .build()
        val dao = database.speciesDao()
        signal = TextNameSignal(ScanNameIndex(dao.allScanNames().map { SpeciesNameHit(SpeciesId(it.speciesId), it.name) }))
        english = dao.summaries(9).associate { it.id to it.name }
    }

    @After
    fun close() {
        database.close()
        context.deleteDatabase(NAME)
    }

    private fun ids(line: String) = signal.analyze(FrameObservation(listOf(line))).candidates.map { it.speciesId.value }.toSet()

    @Test
    fun everySpeciesIsFoundByItsUppercaseName() {
        val missed = english.filter { (id, name) -> id !in ids(name.uppercase()) }
        assertEquals(emptyMap(), missed)
    }

    @Test
    fun everyLongNameSurvivesOneMisreadLetter() {
        val failures = english.filter { (_, name) -> name.length >= LONG_NAME }.filter { (id, name) ->
            val corrupted = name.uppercase().replaceRange(name.length / 2, name.length / 2 + 1, "#")
            ids(corrupted) != setOf(id)
        }
        assertTrue(failures.size <= ALLOWED_TIES, "ambiguous or missed: $failures")
    }

    @Test
    fun confusableDigitsResolve() {
        assertEquals(setOf(25), ids("P1KACHU"))
        assertEquals(setOf(143), ids("5NORLAX"))
        assertEquals(setOf(249), ids("LUG1A"))
    }

    // Scenario: Ordinary game words ignored
    @Test
    fun gameScreenTextHasNoCandidates() {
        val matched = GAME_TEXT.associateWith(::ids).filterValues { it.isNotEmpty() }
        assertEquals(emptyMap(), matched)
    }

    @Test
    fun fuzzyMatchesAreLabelled() {
        val candidate = signal.analyze(FrameObservation(listOf("BULBASAUB"))).candidates.single()
        assertEquals(MatchKind.FUZZY, candidate.kind)
    }

    private companion object {
        const val NAME = "scan-corpus-test.db"
        const val LONG_NAME = 8

        /** A few long names sit one letter from another form of the same family and tie, which reads as no match. */
        const val ALLOWED_TIES = 12

        val GAME_TEXT = listOf(
            "FIGHT BAG POKéMON RUN",
            "What will you do?",
            "Your POKéMON is holding a GOLDEN item",
            "The Wild one fled",
            "TEAM ROCKET BLASTS OFF",
            "Team Magma",
            "Team Aqua",
            "Team Galactic",
            "Team Plasma",
            "Team Flare",
            "Team Skull",
            "Team Yell",
            "A critical hit!",
            "It's super effective!",
            "It's not very effective...",
            "Gained EXP. Points!",
            "Grew to Lv. 16!",
            "learned a new move",
            "SAVE",
            "OPTIONS",
            "EXIT",
            "CONTINUE",
            "NEW GAME",
            "PRESS START",
            "HP PP ATK DEF SPA SPD SPE",
            "Potion Super Potion Hyper Potion Max Potion",
            "Poke Ball Great Ball Ultra Ball Master Ball",
            "Viridian City",
            "Pallet Town",
            "Cerulean Cape",
            "Rustboro City",
            "Lumiose City",
            "Route 1",
            "Professor Oak",
            "Mom",
            "Rival",
            "Gym Leader",
            "Elite Four",
            "Champion",
            "Leaf Stone",
            "Fire Stone",
            "Water Stone",
            "Thunder Stone",
            "Moon Stone",
            "Sun Stone",
            "Tackle Growl Ember Scratch",
            "Thunderbolt",
            "Hyper Beam",
            "Earthquake",
            "Overgrow",
            "Blaze",
            "Torrent",
            "Static",
            "Normal Fire Water Grass Electric Ice Fighting Poison Ground Flying Psychic Bug Rock Ghost Dragon Dark Steel Fairy",
            "Nickname",
            "PLAYER",
            "RED",
            "BLUE",
            "ASH",
            "GARY",
            "Boots",
            "Fluffy",
            "Sparky",
        )
    }
}
