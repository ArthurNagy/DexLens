package com.arthurnagy.dexlens.feature.entry.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation 3 key for the entry feature — the whole public API of :feature:entry:api. Any
 * feature's impl may depend on this module and push it (`navigator.navigate(EntryKey(speciesId))`);
 * :feature:entry:impl renders it. Carries IDs, never objects (modularization guide).
 */
@Serializable
data class EntryKey(
    val speciesId: Int,
) : NavKey
