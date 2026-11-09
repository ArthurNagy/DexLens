package com.arthurnagy.dexlens.core.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.compositionLocalOf

/** The app's shared-transition scope. Null on wide windows, where list and entry are side by side and nothing moves. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Suppress("ComposeCompositionLocalUsage") // Nav 3 entries can only reach the shared scope through a local.
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
