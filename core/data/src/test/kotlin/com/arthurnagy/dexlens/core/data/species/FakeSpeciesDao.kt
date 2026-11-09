package com.arthurnagy.dexlens.core.data.species

import com.arthurnagy.dexlens.core.database.ChainSpeciesRow
import com.arthurnagy.dexlens.core.database.ScanNameRow
import com.arthurnagy.dexlens.core.database.SpeciesDao
import com.arthurnagy.dexlens.core.database.SpeciesSearchRow
import com.arthurnagy.dexlens.core.database.SpeciesSummaryRow
import com.arthurnagy.dexlens.core.database.VarietyAbilityRow
import com.arthurnagy.dexlens.core.database.entity.EggGroupEntity
import com.arthurnagy.dexlens.core.database.entity.EvolutionEntity
import com.arthurnagy.dexlens.core.database.entity.FormEntity
import com.arthurnagy.dexlens.core.database.entity.LanguageEntity
import com.arthurnagy.dexlens.core.database.entity.SpeciesEntity
import com.arthurnagy.dexlens.core.database.entity.SpeciesNameEntity
import com.arthurnagy.dexlens.core.database.entity.VarietyEntity

/** Seeded in-memory DAO. [failure], when set, is thrown by every call. */
internal class FakeSpeciesDao(
    var summaries: List<SpeciesSummaryRow> = emptyList(),
    var species: List<SpeciesEntity> = emptyList(),
    var names: List<SpeciesNameEntity> = emptyList(),
    var languages: List<LanguageEntity> = emptyList(),
    var varieties: List<VarietyEntity> = emptyList(),
    var forms: List<FormEntity> = emptyList(),
    var abilities: List<VarietyAbilityRow> = emptyList(),
    var eggGroups: List<EggGroupEntity> = emptyList(),
    var chain: List<ChainSpeciesRow> = emptyList(),
    var evolutions: List<EvolutionEntity> = emptyList(),
    var searchRows: List<SpeciesSearchRow> = emptyList(),
    var scanRows: List<ScanNameRow> = emptyList(),
    var failure: Exception? = null,
) : SpeciesDao {
    var summaryLoads = 0
        private set

    private fun check() {
        failure?.let { throw it }
    }

    override suspend fun summaries(languageId: Int): List<SpeciesSummaryRow> {
        check()
        summaryLoads++
        return summaries
    }

    override suspend fun searchNames(matchQuery: String): List<SpeciesSearchRow> {
        check()
        return searchRows
    }

    override suspend fun species(speciesId: Int): SpeciesEntity? {
        check()
        return species.firstOrNull { it.id == speciesId }
    }

    override suspend fun speciesNames(speciesId: Int): List<SpeciesNameEntity> = names.filter { it.speciesId == speciesId }

    override suspend fun languages(): List<LanguageEntity> = languages

    override suspend fun varieties(speciesId: Int): List<VarietyEntity> = varieties.filter { it.speciesId == speciesId }

    override suspend fun forms(speciesId: Int): List<FormEntity> = forms

    override suspend fun abilities(speciesId: Int): List<VarietyAbilityRow> = abilities

    override suspend fun eggGroups(speciesId: Int): List<EggGroupEntity> = eggGroups

    override suspend fun chainSpecies(
        chainId: Int,
        languageId: Int,
    ): List<ChainSpeciesRow> = chain

    override suspend fun evolutions(chainId: Int): List<EvolutionEntity> = evolutions

    override suspend fun allScanNames(): List<ScanNameRow> {
        check()
        return scanRows
    }

    override suspend fun speciesCount(): Int = species.size

    override suspend fun speciesIds(): List<Int> {
        check()
        return species.map { it.id }
    }
}
