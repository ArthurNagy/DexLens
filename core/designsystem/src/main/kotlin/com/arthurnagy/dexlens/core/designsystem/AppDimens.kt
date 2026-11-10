package com.arthurnagy.dexlens.core.designsystem

import androidx.compose.ui.unit.dp

/** Sizing tokens, including accessibility minimums: [touchTargetMin] is Material's 48dp target. */
object AppDimens {
    val touchTargetMin = 48.dp
    val iconSm = 16.dp
    val iconMd = 24.dp
    val iconLg = 32.dp

    val listArt = 64.dp
    val evolutionArt = 56.dp
    val headerArt = 208.dp
    val statLabelWidth = 72.dp
    val statValueWidth = 32.dp
    val statBarHeight = 8.dp
    val typeChipHeight = 24.dp
    val artCorner = 16.dp
    val cardCorner = 20.dp
    val maxContentWidth = 640.dp
    val genderBarHeight = 8.dp
    val treeIndent = 24.dp
    val aimBoxCorner = 20.dp
    val aimBoxStroke = 3.dp
    val aimBoxCornerLength = 36.dp

    /** Height of the button row at the top of the home screen's scanner area. */
    val scannerButtonRowHeight = 56.dp

    /** Width of that row. The aiming box stays this far from both sides, so it stays centred and clear of the buttons. */
    val scannerButtonRowWidth = 104.dp

    /** Material's bottom sheet drag handle, with its padding. */
    val sheetDragHandleHeight = 48.dp

    /** Width over height of the scanner area below the status bar. */
    val scannerAreaAspectRatio = 3f / 2f

    /** The most of the window's height the scanner area may take, so short windows keep the list reachable. */
    val scannerAreaMaxHeightFraction = 0.45f

    val skeletonLineHeight = 16.dp
    val skeletonLineShort = 64.dp
    val skeletonLineMedium = 120.dp
    val skeletonLineLong = 180.dp
    val skeletonSectionHeight = 120.dp
}
