package com.arthurnagy.dexlens.core.data.content

import app.cash.turbine.test
import com.arthurnagy.dexlens.core.database.GameVersionDao
import com.arthurnagy.dexlens.core.database.entity.FlavorTextEntity
import com.arthurnagy.dexlens.core.database.entity.GameVersionEntity
import com.arthurnagy.dexlens.core.model.FlavorEntry
import com.arthurnagy.dexlens.core.model.SpeciesId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

internal class OfflineFlavorTextRepositoryTest {
    private val dao = FakeContentDao()
    private val versions = GameVersionDao { listOf(GameVersionEntity(1, "Red", 1), GameVersionEntity(2, "Blue", 1)) }

    private fun repository(language: Int = 9) = OfflineFlavorTextRepository(dao, versions) { language }

    // Scenario: Flavor text is imported
    @Test
    fun groupsIdenticalTextsByGameNewestFirst() = runTest {
        dao.upsertAll(listOf(FlavorTextEntity(1, 1, 9, "Same"), FlavorTextEntity(1, 2, 9, "Same"), FlavorTextEntity(1, 3, 9, "Other")))
        val entries = repository().observeFlavorEntries(SpeciesId(1)).first()
        assertEquals(listOf(FlavorEntry("Other", emptyList()), FlavorEntry("Same", listOf("Red", "Blue"))), entries)
    }

    @Test
    fun fallsBackToEnglish() = runTest {
        dao.upsertAll(listOf(FlavorTextEntity(1, 1, 9, "English only")))
        assertEquals("English only", repository(language = 5).observeFlavorEntries(SpeciesId(1)).first().single().text)
    }

    @Test
    fun prefersTheDisplayLanguage() = runTest {
        dao.upsertAll(listOf(FlavorTextEntity(1, 1, 9, "English"), FlavorTextEntity(1, 1, 5, "Français")))
        assertEquals("Français", repository(language = 5).observeFlavorEntries(SpeciesId(1)).first().single().text)
    }

    // Scenario: Flavor text appears in an open entry
    @Test
    fun emitsAgainWhenTextIsImported() = runTest {
        repository().observeFlavorEntries(SpeciesId(1)).test {
            assertEquals(emptyList(), awaitItem())
            dao.upsertAll(listOf(FlavorTextEntity(1, 1, 9, "A seed.")))
            assertEquals(listOf(FlavorEntry("A seed.", listOf("Red"))), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun aDatabaseFailureReadsAsNoText() = runTest {
        val failing = OfflineFlavorTextRepository(dao, GameVersionDao { throw IllegalStateException("broken") }) { 9 }
        assertEquals(emptyList(), failing.observeFlavorEntries(SpeciesId(1)).first())
    }
}
