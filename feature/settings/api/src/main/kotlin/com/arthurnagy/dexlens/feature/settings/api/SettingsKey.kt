package com.arthurnagy.dexlens.feature.settings.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** The settings screen. */
@Serializable
data object SettingsKey : NavKey

/** The one-time first-launch offer of the optional download, shown as a dialog. */
@Serializable
data object ContentOfferKey : NavKey

/** The list of open-source libraries bundled in the app, a list-detail list. */
@Serializable
data object LicensesKey : NavKey

/** One bundled library's licence and notices, the detail beside [LicensesKey]. [libraryId] is its `group:artifact`. */
@Serializable
data class LicenseDetailKey(
    val libraryId: String,
) : NavKey
