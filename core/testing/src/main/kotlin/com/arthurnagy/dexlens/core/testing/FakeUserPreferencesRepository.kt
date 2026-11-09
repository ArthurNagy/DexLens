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
) : UserPreferencesRepository {
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
}
