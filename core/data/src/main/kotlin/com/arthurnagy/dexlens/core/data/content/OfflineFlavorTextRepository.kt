package com.arthurnagy.dexlens.core.data.content

import com.arthurnagy.dexlens.core.data.species.DisplayLanguage
import com.arthurnagy.dexlens.core.data.species.ENGLISH
import com.arthurnagy.dexlens.core.database.ContentDao
import com.arthurnagy.dexlens.core.database.GameVersionDao
import com.arthurnagy.dexlens.core.database.entity.FlavorTextEntity
import com.arthurnagy.dexlens.core.domain.content.FlavorTextRepository
import com.arthurnagy.dexlens.core.model.FlavorEntry
import com.arthurnagy.dexlens.core.model.SpeciesId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

internal class OfflineFlavorTextRepository
    @Inject
    constructor(
        private val contentDao: ContentDao,
        private val gameVersionDao: GameVersionDao,
        private val displayLanguage: DisplayLanguage,
    ) : FlavorTextRepository {
        override fun observeFlavorEntries(id: SpeciesId): Flow<List<FlavorEntry>> = flow {
            val language = displayLanguage.languageId()
            val games = gameVersionDao.gameVersions().associate { it.id to it.name }
            emitAll(contentDao.observeFlavorText(id.value, setOf(language, ENGLISH)).map { rows -> entries(rows, language, games) })
        }.catch { error ->
            Timber.w(error, "could not read flavor text")
            emit(emptyList())
        }

        /** The display language's text, or English when a language has none. */
        private fun entries(
            rows: List<FlavorTextEntity>,
            language: Int,
            games: Map<Int, String>,
        ): List<FlavorEntry> = rows
            .filter { it.languageId == language }
            .ifEmpty { rows.filter { it.languageId == ENGLISH } }
            .groupBy { it.text }
            .map { (text, sameText) -> FlavorEntry(text, sameText.mapNotNull { games[it.versionId] }) }
    }
