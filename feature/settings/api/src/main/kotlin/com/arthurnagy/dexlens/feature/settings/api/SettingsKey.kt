package com.arthurnagy.dexlens.feature.settings.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** The settings screen. */
@Serializable
data object SettingsKey : NavKey

/** The one-time first-launch offer of the optional download, shown as a dialog. */
@Serializable
data object ContentOfferKey : NavKey
