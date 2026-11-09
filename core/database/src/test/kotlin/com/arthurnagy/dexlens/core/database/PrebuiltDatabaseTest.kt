package com.arthurnagy.dexlens.core.database

import android.content.Context
import androidx.room3.Room
import androidx.room3.useReaderConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Opens the real generated asset through Room on the host-JVM bundled driver, so FTS5 is available. */
@RunWith(AndroidJUnit4::class)
internal class PrebuiltDatabaseTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private lateinit var database: DexDatabase
    private val dao get() = database.speciesDao()

    @Before
    fun open() {
        context.deleteDatabase(NAME)
        database = Room
            .databaseBuilder<DexDatabase>(context, NAME)
            .createFromAsset("databases/dexlens.db")
            .setDriver(BundledSQLiteDriver())
            .build()
    }

    @After
    fun close() {
        database.close()
        context.deleteDatabase(NAME)
    }

    private suspend fun scalar(sql: String): Long = database.useReaderConnection { connection ->
        connection.usePrepared(sql) { statement ->
            statement.step()
            statement.getLong(0)
        }
    }

    private suspend fun search(match: String) = dao.searchNames(match).map { it.speciesId }.toSet()

    // Scenario: All species present offline
    @Test
    fun allSpeciesPresent() = runTest {
        assertEquals(1025, dao.speciesCount())
        val summaries = dao.summaries(ENGLISH)
        assertEquals(1025, summaries.size)
        assertEquals(1..1025, summaries.first().id..summaries.last().id)
        assertEquals(
            0L,
            scalar(
                "SELECT COUNT(*) FROM variety WHERE is_default = 1 AND (hp + attack + defense + special_attack + special_defense + speed) = 0",
            ),
        )
        assertEquals(
            0L,
            scalar(
                "SELECT COUNT(*) FROM variety v WHERE v.is_default = 1 AND NOT EXISTS (SELECT 1 FROM variety_ability a WHERE a.variety_id = v.id)",
            ),
        )
        assertTrue(dao.abilities(1).isNotEmpty())
        assertEquals(listOf("Grass", "Monster"), dao.eggGroups(1).map { it.name }.sorted())
    }

    // Scenario: Names in every language
    @Test
    fun namesInEveryLanguage() = runTest {
        assertEquals(12, dao.speciesNames(25).size)
        assertEquals(12, dao.languages().size)
        assertEquals("Pikachu", dao.summaries(ENGLISH).first { it.id == 25 }.name)
        assertEquals("ピカチュウ", dao.summaries(JAPANESE).first { it.id == 25 }.name)
        assertEquals("Mouse Pokémon", dao.speciesNames(25).first { it.languageId == ENGLISH }.genus)
    }

    // Scenario: Tricky names preserved
    @Test
    fun trickyNames() = runTest {
        val names = dao.summaries(ENGLISH).associate { it.id to it.name }
        assertEquals("Nidoran♀", names[29])
        assertEquals("Mr. Mime", names[122])
        assertEquals("Farfetch’d", names[83])
        assertEquals("Flabébé", names[669])
        assertEquals("Type: Null", names[772])
        assertEquals("Ho-Oh", names[250])
        assertEquals("Porygon-Z", names[474])
        assertEquals("Pecharunt", names[1025])
    }

    // Scenario: Ability has PokeAPI prose
    @Test
    fun abilitiesUsePokeApiProse() = runTest {
        assertEquals(0L, scalar("SELECT COUNT(*) FROM ability WHERE short_effect IS NULL OR effect IS NULL"))
        assertFalse("markup removed", scalar("SELECT COUNT(*) FROM ability WHERE effect LIKE '%]{%'") > 0)
        val overgrow = dao.abilities(1).first { it.name == "Overgrow" }
        assertEquals("Strengthens Grass moves to inflict 1.5× damage at 1/3 max HP or less.", overgrow.shortEffect)
        assertTrue("hidden ability", dao.abilities(1).any { it.name == "Chlorophyll" && it.isHidden })
    }

    // Scenario: Database has no flavor text
    @Test
    fun noGameContentBundled() = runTest {
        assertEquals(0L, scalar("SELECT COUNT(*) FROM sqlite_master WHERE name LIKE '%flavor%'"))
        assertEquals(
            0L,
            scalar(
                "SELECT COUNT(*) FROM sqlite_master m, pragma_table_info(m.name) p WHERE m.type = 'table' AND m.name NOT LIKE 'species_name_fts_%' AND p.type = 'BLOB'",
            ),
        )
    }

    @Test
    fun formsAndVarieties() = runTest {
        val varieties = dao.varieties(6)
        assertEquals(listOf("Charizard", "Mega Charizard X", "Mega Charizard Y", "Gigantamax Charizard"), varieties.map { it.name })
        assertEquals(1, varieties.count { it.isDefault })
        assertTrue(dao.forms(201).size >= 26)
    }

    @Test
    fun evolutionLineSpecies() = runTest {
        val chain = checkNotNull(dao.species(133)).evolutionChainId
        val species = dao.chainSpecies(chain, ENGLISH)
        assertTrue(species.any { it.id == 134 && it.evolvesFromSpeciesId == 133 })
        assertEquals(133, species.first().id)
    }

    @Test
    fun scanNamesExcludeRomaji() = runTest {
        val names = dao.allScanNames()
        assertEquals(1025 * 11, names.size)
        assertTrue(names.any { it.speciesId == 1 && it.name == "Bulbizarre" })
        assertFalse(names.any { it.name == "Fushigidane" })
        assertTrue(database.gameVersionDao().gameVersions().any { it.name == "Red" })
    }

    // Scenario: Level evolution
    @Test
    fun levelEvolution() = runTest {
        val species = checkNotNull(dao.species(2))
        val evolution = dao.evolutions(species.evolutionChainId).single { it.speciesId == 2 }
        assertEquals(1, evolution.fromSpeciesId)
        assertEquals("level-up", evolution.trigger)
        assertEquals(16, evolution.minLevel)
    }

    // Scenario: Item evolution
    @Test
    fun itemEvolution() = runTest {
        val species = checkNotNull(dao.species(134))
        val evolution = dao.evolutions(species.evolutionChainId).single { it.speciesId == 134 }
        assertEquals(133, evolution.fromSpeciesId)
        assertEquals("use-item", evolution.trigger)
        assertEquals("Water Stone", evolution.item)
    }

    // Scenario: Chart is complete
    @Test
    fun typeChartComplete() = runTest {
        val chart = database.typeChartDao().typeChart()
        assertEquals(324, chart.size)
        assertEquals(200, chart.first { it.attackingType == WATER && it.defendingType == FIRE }.factor)
        assertEquals(0, chart.first { it.attackingType == NORMAL && it.defendingType == GHOST }.factor)
    }

    // Scenario: Prefix without accents finds accented name
    @Test
    fun accentlessPrefix() = runTest {
        assertEquals(setOf(669), search("flabebe*"))
    }

    // Scenario: Punctuation is ignored
    @Test
    fun punctuationIgnored() = runTest {
        assertTrue(122 in search("\"mr\"* \"mime\"*"))
        assertTrue(83 in search("farfetchd*"))
        assertTrue(772 in search("\"type\"* \"null\"*"))
        assertTrue(250 in search("hooh*"))
        assertTrue(474 in search("porygonz*"))
    }

    // Scenario: Other-language name found
    @Test
    fun otherLanguageNames() = runTest {
        assertTrue(1 in search("bulbizarre*"))
        assertTrue(1 in search("フシギダネ*"))
    }

    // Scenario: Prebuilt file passes validation
    @Test
    fun validatesOnReopen() = runTest {
        assertEquals(1025, dao.speciesCount())
        database.close()
        database = Room
            .databaseBuilder<DexDatabase>(context, NAME)
            .createFromAsset("databases/dexlens.db")
            .setDriver(BundledSQLiteDriver())
            .build()
        assertEquals(1025, dao.speciesCount())
    }

    private companion object {
        const val NAME = "dexlens-test.db"
        const val ENGLISH = 9
        const val JAPANESE = 11
        const val NORMAL = 1
        const val FIRE = 10
        const val WATER = 11
        const val GHOST = 8
    }
}
