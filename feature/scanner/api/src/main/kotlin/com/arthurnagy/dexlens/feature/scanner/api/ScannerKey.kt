package com.arthurnagy.dexlens.feature.scanner.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation 3 key for the scanner feature — the whole public API of :feature:scanner:api. Any
 * feature's impl may depend on this module and push it (`navigator.navigate(ScannerKey)`);
 * :feature:scanner:impl renders it. Carries IDs, never objects (modularization guide).
 */
@Serializable
data object ScannerKey : NavKey
