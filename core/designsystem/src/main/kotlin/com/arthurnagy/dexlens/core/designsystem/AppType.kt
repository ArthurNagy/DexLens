package com.arthurnagy.dexlens.core.designsystem

import androidx.compose.material3.Typography

/**
 * DexLens's type scale, handed to MaterialTheme next to the colour schemes in AppColors.kt. It is Material's default
 * scale for now: a custom font would be a FontFamily declared here and applied to each style with `copy(fontFamily = …)`.
 * Composables ALWAYS read text styles from MaterialTheme.typography — never hard-code `.sp`.
 */
internal val AppTypography: Typography = Typography()
