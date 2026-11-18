package com.arthurnagy.dexlens.feature.entry.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.orbitmvi.orbit.compose.collectAsState

@Composable
internal fun EntryScreen(
    viewModel: EntryViewModel,
    onNavigateBack: (() -> Unit)?,
    showBackArrow: Boolean,
    onOpenSpecies: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.collectAsState()
    EntryContent(
        state = state,
        onNavigateBack = onNavigateBack,
        showBackArrow = showBackArrow,
        onOpenSpecies = onOpenSpecies,
        onSelectVariety = viewModel::onSelectVariety,
        onSelectGame = viewModel::onSelectGame,
        modifier = modifier,
    )
}
