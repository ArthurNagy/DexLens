package com.arthurnagy.dexlens.feature.entry.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.orbitmvi.orbit.compose.collectAsState

@Composable
internal fun EntryScreen(
    viewModel: EntryViewModel,
    onNavigateBack: (() -> Unit)?,
    onOpenSpecies: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.collectAsState()
    EntryContent(
        state = state,
        onNavigateBack = onNavigateBack,
        onOpenSpecies = onOpenSpecies,
        onSelectVariety = viewModel::onSelectVariety,
        onSelectGame = viewModel::onSelectGame,
        modifier = modifier,
    )
}
