package com.arthurnagy.dexlens.core.testing

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.species.SpeciesRepository
import com.arthurnagy.dexlens.core.model.SpeciesDetail
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.model.SpeciesNameHit
import com.arthurnagy.dexlens.core.model.SpeciesSummary
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

/** Behaves like the real repository over seeded data. [nextError] fails the next call of any method. */
class FakeSpeciesRepository(
    var summaries: List<SpeciesSummary> = emptyList(),
    var hits: Map<String, List<SpeciesNameHit>> = emptyMap(),
    var details: Map<SpeciesId, SpeciesDetail> = emptyMap(),
    var scanNames: List<SpeciesNameHit> = emptyList(),
) : SpeciesRepository {
    var nextError: DomainError? = null

    private fun <T> respond(value: () -> Result<T, DomainError>): Result<T, DomainError> {
        nextError?.let {
            nextError = null
            return Err(it)
        }
        return value()
    }

    override suspend fun summaries(): Result<List<SpeciesSummary>, DomainError> = respond { Ok(summaries) }

    override suspend fun searchNames(ftsQuery: String): Result<List<SpeciesNameHit>, DomainError> = respond { Ok(hits[ftsQuery].orEmpty()) }

    override suspend fun detail(id: SpeciesId): Result<SpeciesDetail, DomainError> = respond {
        details[id]?.let { Ok(it) } ?: Err(DomainError.NotFound)
    }

    override suspend fun scanNames(): Result<List<SpeciesNameHit>, DomainError> = respond { Ok(scanNames) }
}
