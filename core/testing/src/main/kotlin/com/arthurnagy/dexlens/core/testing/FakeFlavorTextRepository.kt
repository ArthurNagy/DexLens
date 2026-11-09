package com.arthurnagy.dexlens.core.testing

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.content.FlavorTextRepository
import com.arthurnagy.dexlens.core.model.FlavorEntry
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result

class FakeFlavorTextRepository(
    var entries: List<FlavorEntry> = emptyList(),
) : FlavorTextRepository {
    var nextError: DomainError? = null

    override suspend fun flavorEntries(id: SpeciesId): Result<List<FlavorEntry>, DomainError> {
        nextError?.let {
            nextError = null
            return Err(it)
        }
        return Ok(entries)
    }
}
