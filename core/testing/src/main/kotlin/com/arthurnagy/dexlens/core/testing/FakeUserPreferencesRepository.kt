package com.arthurnagy.dexlens.core.testing

import com.arthurnagy.dexlens.core.domain.DomainError
import com.arthurnagy.dexlens.core.domain.UserPreferencesRepository
import com.arthurnagy.dexlens.core.model.ThemeMode
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Stateful in-memory [UserPreferencesRepository]; set [failNextWrite] to make the next write fail once. */
class FakeUserPreferencesRepository(
    initialThemeMode: ThemeMode,
    initialOfferShown: Boolean = false,
    initialScannerEnabled: Boolean = true,
    initialPreferredGame: Int? = null,
) : UserPreferencesRepository {
    private val game = MutableStateFlow(initialPreferredGame)

    private val scannerSwitch = MutableStateFlow(initialScannerEnabled)

    private val offerShown = MutableStateFlow(initialOfferShown)

    private val stored = MutableStateFlow(initialThemeMode)

    var failNextWrite: DomainError? = null

    override val themeMode: Flow<ThemeMode> = stored

    override suspend fun setThemeMode(mode: ThemeMode): Result<Unit, DomainError> {
        failNextWrite?.let { error ->
            failNextWrite = null
            return Err(error)
        }
        stored.value = mode
        return Ok(Unit)
    }

    override val contentOfferShown: Flow<Boolean> = offerShown

    override suspend fun setContentOfferShown(shown: Boolean): Result<Unit, DomainError> {
        failNextWrite?.let { error ->
            failNextWrite = null
            return Err(error)
        }
        offerShown.value = shown
        return Ok(Unit)
    }

    override val scannerEnabled: Flow<Boolean> = scannerSwitch

    override suspend fun setScannerEnabled(enabled: Boolean): Result<Unit, DomainError> {
        failNextWrite?.let { error ->
            failNextWrite = null
            return Err(error)
        }
        scannerSwitch.value = enabled
        return Ok(Unit)
    }

    override val preferredGame: Flow<Int?> = game

    override suspend fun setPreferredGame(gameId: Int): Result<Unit, DomainError> {
        failNextWrite?.let { error ->
            failNextWrite = null
            return Err(error)
        }
        game.value = gameId
        return Ok(Unit)
    }
}
