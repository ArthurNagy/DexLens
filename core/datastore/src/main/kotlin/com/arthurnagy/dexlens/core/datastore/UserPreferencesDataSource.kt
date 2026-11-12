package com.arthurnagy.dexlens.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.arthurnagy.dexlens.core.model.ThemeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import java.io.IOException
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

/**
 * The user-preferences DataStore. Public because :core:data's repository wraps it; nothing else
 * may depend on :core:datastore. Reads never throw: an unreadable file reads as the defaults and is
 * read again a little later — `catch { }` alone would complete the flow, and the theme would stop
 * following later changes (a corrupt file is replaced by DataStoreModule's corruption handler). Writes
 * may throw — the repository turns that into a `DomainError`.
 */
class UserPreferencesDataSource
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        private val preferences: Flow<Preferences> = dataStore.data.retryWhen { cause, attempt ->
            if (cause is IOException) {
                emit(emptyPreferences())
                delay((attempt + 1).coerceAtMost(MAX_RETRY_DELAY_SECONDS).seconds) // 1 s, 2 s, … 30 s
                true
            } else {
                false
            }
        }

        val themeMode: Flow<ThemeMode> =
            preferences
                .map { preferences ->
                    preferences[THEME_MODE]?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } } ?: ThemeMode.SYSTEM
                }

        val contentOfferShown: Flow<Boolean> = preferences.map { it[CONTENT_OFFER_SHOWN] ?: false }

        val scannerEnabled: Flow<Boolean> = preferences.map { it[SCANNER_ENABLED] ?: true }

        val preferredGame: Flow<Int?> = preferences.map { it[PREFERRED_GAME] }

        val recognitionScoresShown: Flow<Boolean> = preferences.map { it[RECOGNITION_SCORES_SHOWN] ?: false }

        suspend fun setRecognitionScoresShown(shown: Boolean) {
            dataStore.edit { it[RECOGNITION_SCORES_SHOWN] = shown }
        }

        suspend fun setPreferredGame(gameId: Int) {
            dataStore.edit { it[PREFERRED_GAME] = gameId }
        }

        suspend fun setScannerEnabled(enabled: Boolean) {
            dataStore.edit { it[SCANNER_ENABLED] = enabled }
        }

        suspend fun setContentOfferShown(shown: Boolean) {
            dataStore.edit { it[CONTENT_OFFER_SHOWN] = shown }
        }

        suspend fun setThemeMode(mode: ThemeMode) {
            dataStore.edit { it[THEME_MODE] = mode.name }
        }

        private companion object {
            val THEME_MODE = stringPreferencesKey("theme_mode")
            val CONTENT_OFFER_SHOWN = booleanPreferencesKey("content_offer_shown")
            val SCANNER_ENABLED = booleanPreferencesKey("scanner_enabled")
            val PREFERRED_GAME = intPreferencesKey("preferred_game")
            val RECOGNITION_SCORES_SHOWN = booleanPreferencesKey("recognition_scores_shown")
            const val MAX_RETRY_DELAY_SECONDS = 30L
        }
    }
