package com.arthurnagy.dexlens.core.domain.species

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.UseCase
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.model.SpeciesNameHit
import com.arthurnagy.dexlens.core.model.SpeciesSearchResult
import com.arthurnagy.dexlens.core.model.SpeciesSummary
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.map
import javax.inject.Inject

/**
 * Finds species by name prefix in any language, or by Pokédex number. A blank query returns the whole list, so the
 * home screen has one code path.
 */
class SearchSpeciesUseCase
    @Inject
    constructor(
        private val repository: SpeciesRepository,
    ) : UseCase {
        suspend operator fun invoke(query: String): Result<List<SpeciesSearchResult>, DomainError> {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) return repository.summaries().map { all -> all.map { SpeciesSearchResult(it, matchedName = null) } }
            NUMBER.matchEntire(trimmed)?.let { match -> return searchByNumber(digits = match.groupValues[1]) }
            val ftsQuery = SpeciesNameNormalizer.toFtsPrefixQuery(trimmed) ?: return Ok(emptyList())
            val compactQuery = SpeciesNameNormalizer.compact(trimmed)
            return repository.summaries().andThen { summaries ->
                val byId = summaries.associateBy { it.id }
                repository.searchNames(ftsQuery).map { hits -> rank(hits, byId, compactQuery) }
            }
        }

        private suspend fun searchByNumber(digits: String): Result<List<SpeciesSearchResult>, DomainError> =
            repository.summaries().map { all ->
                val exactOnly = digits.length >= PADDED_LENGTH || digits.startsWith("0")
                val number = digits.toIntOrNull()
                all
                    .filter { summary ->
                        summary.id.value == number || (!exactOnly && summary.id.value.toString().startsWith(digits))
                    }.sortedWith(compareBy({ it.id.value != number }, { it.id.value }))
                    .map { SpeciesSearchResult(it, matchedName = null) }
            }

        private fun rank(
            hits: List<SpeciesNameHit>,
            summaries: Map<SpeciesId, SpeciesSummary>,
            compactQuery: String,
        ): List<SpeciesSearchResult> = hits
            .groupBy { it.speciesId }
            .mapNotNull { (id, names) ->
                val summary = summaries[id] ?: return@mapNotNull null
                val best = names.minBy { matchRank(it.name, compactQuery) }
                val displayedNameMatches = names.any { it.name == summary.name }
                val matchedName = best.name.takeUnless { displayedNameMatches || it == summary.name }
                Triple(
                    summary,
                    matchedName,
                    if (displayedNameMatches) matchRank(summary.name, compactQuery) else matchRank(best.name, compactQuery),
                )
            }.sortedWith(compareBy({ it.third }, { it.first.id.value }))
            .map { (summary, matchedName, _) -> SpeciesSearchResult(summary, matchedName) }

        /** Lower is better: whole name, then name prefix, then a later word. */
        private fun matchRank(
            name: String,
            compactQuery: String,
        ): Int {
            val compactName = SpeciesNameNormalizer.compact(name)
            return when {
                compactName == compactQuery -> RANK_EXACT
                compactName.startsWith(compactQuery) -> RANK_PREFIX
                else -> RANK_WORD
            }
        }

        private companion object {
            val NUMBER = Regex("""#?(\d{1,4})""")
            const val PADDED_LENGTH = 3
            const val RANK_EXACT = 0
            const val RANK_PREFIX = 1
            const val RANK_WORD = 2
        }
    }
