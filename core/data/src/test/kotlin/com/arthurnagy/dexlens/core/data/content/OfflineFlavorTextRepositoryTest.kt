package com.arthurnagy.dexlens.core.data.content

import com.arthurnagy.dexlens.core.database.GameVersionDao
import com.arthurnagy.dexlens.core.database.entity.FlavorTextEntity
import com.arthurnagy.dexlens.core.database.entity.GameVersionEntity
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.model.FlavorEntry
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.getOrThrow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

internal class OfflineFlavorTextRepositoryTest {
    private val dao = FakeContentDao()
    private val versions = GameVersionDao { listOf(GameVersionEntity(1, "Red"), GameVersionEntity(2, "Blue")) }

    private fun repository(language: Int = 9) = OfflineFlavorTextRepository(dao, versions) { language }

    @Test
    fun groupsIdenticalTextsByGame() = runTest {
        dao.upsertAll(listOf(FlavorTextEntity(1, 1, 9, "Same"), FlavorTextEntity(1, 2, 9, "Same"), FlavorTextEntity(1, 3, 9, "Other")))
        val entries = repository().flavorEntries(SpeciesId(1)).getOrThrow { error("unexpected") }
        assertEquals(listOf(FlavorEntry("Same", listOf("Red", "Blue")), FlavorEntry("Other", emptyList())), entries)
    }

    @Test
    fun fallsBackToEnglish() = runTest {
        dao.upsertAll(listOf(FlavorTextEntity(1, 1, 9, "English only")))
        assertEquals("English only", repository(language = 5).flavorEntries(SpeciesId(1)).getOrThrow { error("unexpected") }.single().text)
    }

    @Test
    fun aDatabaseFailureBecomesADomainError() = runTest {
        val failing = OfflineFlavorTextRepository(dao, GameVersionDao { throw IllegalStateException("broken") }) { 9 }
        assertEquals(Err(DomainError.Unknown("broken")), failing.flavorEntries(SpeciesId(1)))
    }
}
