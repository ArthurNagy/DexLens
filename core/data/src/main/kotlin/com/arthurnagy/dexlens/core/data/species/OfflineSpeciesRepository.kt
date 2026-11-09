package com.arthurnagy.dexlens.core.data.species

import com.arthurnagy.dexlens.core.data.catchStorage
import com.arthurnagy.dexlens.core.database.SpeciesDao
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.species.SpeciesRepository
import com.arthurnagy.dexlens.core.model.GenderRatio
import com.arthurnagy.dexlens.core.model.LocalizedName
import com.arthurnagy.dexlens.core.model.SpeciesDetail
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.model.SpeciesNameHit
import com.arthurnagy.dexlens.core.model.SpeciesSummary
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.toErrorIfNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class OfflineSpeciesRepository
    @Inject
    constructor(
        private val dao: SpeciesDao,
        private val displayLanguage: DisplayLanguage,
    ) : SpeciesRepository {
        private val cacheLock = Mutex()
        private var cached: Pair<Int, List<SpeciesSummary>>? = null

        override suspend fun summaries(): Result<List<SpeciesSummary>, DomainError> = catchStorage { loadSummaries() }

        private suspend fun loadSummaries(): List<SpeciesSummary> = cacheLock.withLock {
            val language = displayLanguage.languageId()
            cached?.takeIf { it.first == language }?.second ?: dao.summaries(language).map { it.toModel() }.also { cached = language to it }
        }

        override suspend fun searchNames(ftsQuery: String): Result<List<SpeciesNameHit>, DomainError> = catchStorage {
            dao.searchNames(ftsQuery).map { SpeciesNameHit(SpeciesId(it.speciesId), it.name) }
        }

        override suspend fun scanNames(): Result<List<SpeciesNameHit>, DomainError> = catchStorage {
            dao.allScanNames().map { SpeciesNameHit(SpeciesId(it.speciesId), it.name) }
        }

        override suspend fun detail(id: SpeciesId): Result<SpeciesDetail, DomainError> = catchStorage {
            loadDetail(id)
        }.toErrorIfNull { DomainError.NotFound }

        private suspend fun loadDetail(id: SpeciesId): SpeciesDetail? {
            val species = dao.species(id.value) ?: return null
            val language = displayLanguage.languageId()
            val summary = loadSummaries().firstOrNull { it.id == id } ?: return null
            val names = dao.speciesNames(id.value)
            val genus = names.firstOrNull { it.languageId == language }?.genus ?: names.firstOrNull { it.languageId == ENGLISH }?.genus
            val languageLabels = dao.languages().associate { it.id to it.name }
            val abilitiesByVariety = dao.abilities(id.value).groupBy { it.varietyId }
            val formsByVariety = dao.forms(id.value).groupBy { it.varietyId }
            val chain = dao.chainSpecies(species.evolutionChainId, language)
            val nameBySpecies = loadSummaries().associate { it.id.value to it.name }
            return SpeciesDetail(
                summary = summary,
                genus = genus,
                generation = species.generation,
                genderRatio = GenderRatio(species.genderRate.takeIf { it >= 0 }),
                eggGroups = dao.eggGroups(id.value).map { it.name },
                varieties = dao.varieties(id.value).map { variety ->
                    variety.toModel(
                        abilities = abilitiesByVariety[variety.id].orEmpty().map { it.toModel() },
                        forms = formsByVariety[variety.id].orEmpty().map { it.toModel() },
                    )
                },
                evolutionLine = buildEvolutionTree(chain, dao.evolutions(species.evolutionChainId), nameBySpecies),
                otherNames = names
                    .filter { it.languageId != language && it.languageId != ROMAJI }
                    .map { LocalizedName(languageLabels[it.languageId].orEmpty(), it.name) }
                    .sortedBy { it.language },
            )
        }

        private companion object {
            const val ROMAJI = 2
        }
    }
