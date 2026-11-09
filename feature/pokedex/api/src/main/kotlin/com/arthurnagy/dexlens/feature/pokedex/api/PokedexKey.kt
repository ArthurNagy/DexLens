package com.arthurnagy.dexlens.feature.pokedex.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation 3 key for the pokedex feature — the whole public API of :feature:pokedex:api. Any
 * feature's impl may depend on this module and push it (`navigator.navigate(PokedexKey)`);
 * :feature:pokedex:impl renders it. Carries IDs, never objects (modularization guide).
 */
@Serializable
data object PokedexKey : NavKey
