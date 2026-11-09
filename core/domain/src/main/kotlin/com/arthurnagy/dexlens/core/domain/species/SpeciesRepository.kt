package com.arthurnagy.dexlens.core.domain.species

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.model.SpeciesDetail
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.arthurnagy.dexlens.core.model.SpeciesNameHit
import com.arthurnagy.dexlens.core.model.SpeciesSummary
import com.github.michaelbull.result.Result

interface SpeciesRepository {
    /** Every species in Pokédex order, in the display language. */
    suspend fun summaries(): Result<List<SpeciesSummary>, DomainError>

    /** The names matching an FTS5 prefix query, in any language. */
    suspend fun searchNames(ftsQuery: String): Result<List<SpeciesNameHit>, DomainError>

    suspend fun detail(id: SpeciesId): Result<SpeciesDetail, DomainError>

    /** Names in every script a game might display. The camera's matcher builds its index from these. */
    suspend fun scanNames(): Result<List<SpeciesNameHit>, DomainError>
}
