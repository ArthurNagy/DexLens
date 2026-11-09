package com.arthurnagy.dexlens.core.data.content

import com.arthurnagy.dexlens.core.data.catchStorage
import com.arthurnagy.dexlens.core.data.species.DisplayLanguage
import com.arthurnagy.dexlens.core.data.species.ENGLISH
import com.arthurnagy.dexlens.core.database.ContentDao
import com.arthurnagy.dexlens.core.database.GameVersionDao
import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.content.FlavorTextRepository
import com.arthurnagy.dexlens.core.model.FlavorEntry
import com.arthurnagy.dexlens.core.model.SpeciesId
import com.github.michaelbull.result.Result
import javax.inject.Inject

internal class OfflineFlavorTextRepository
    @Inject
    constructor(
        private val contentDao: ContentDao,
        private val gameVersionDao: GameVersionDao,
        private val displayLanguage: DisplayLanguage,
    ) : FlavorTextRepository {
        override suspend fun flavorEntries(id: SpeciesId): Result<List<FlavorEntry>, DomainError> = catchStorage {
            val games = gameVersionDao.gameVersions().associate { it.id to it.name }
            val rows = contentDao.flavorText(id.value, displayLanguage.languageId()).ifEmpty { contentDao.flavorText(id.value, ENGLISH) }
            rows
                .groupBy { it.text }
                .map { (text, sameText) -> FlavorEntry(text, sameText.mapNotNull { games[it.versionId] }) }
        }
    }
