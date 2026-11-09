package com.arthurnagy.dexlens.feature.pokedex.impl

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopSearchBar
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.arthurnagy.dexlens.core.designsystem.AppDimens
import com.arthurnagy.dexlens.core.designsystem.AppSpacing
import com.arthurnagy.dexlens.core.model.SpeciesSearchResult
import com.arthurnagy.dexlens.core.ui.LoadingState
import com.arthurnagy.dexlens.core.ui.LocalSharedTransitionScope
import com.arthurnagy.dexlens.core.ui.MessageState
import com.arthurnagy.dexlens.core.ui.SpeciesArt
import com.arthurnagy.dexlens.core.ui.TypeChipRow
import com.arthurnagy.dexlens.core.ui.labelRes
import com.arthurnagy.dexlens.core.ui.listText
import com.arthurnagy.dexlens.core.ui.speciesNumberLabel
import com.arthurnagy.dexlens.core.ui.toMessage
import org.orbitmvi.orbit.compose.collectAsState
import com.arthurnagy.dexlens.core.ui.R as CoreUiR

@Composable
internal fun PokedexScreen(
    onOpenSpecies: (Int) -> Unit,
    canNavigateBack: Boolean,
    modifier: Modifier = Modifier,
    viewModel: PokedexViewModel = hiltViewModel(),
) {
    val state by viewModel.collectAsState()
    val textFieldState = rememberTextFieldState()

    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }.collect(viewModel::onQueryChanged)
    }
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        // With an entry open beside the list, Back belongs to the entry.
        isBackEnabled = textFieldState.text.isNotEmpty() && !canNavigateBack,
        onBackCompleted = { textFieldState.clearText() },
    )
    val keyboard = LocalSoftwareKeyboardController.current

    PokedexContent(
        state = state,
        textFieldState = textFieldState,
        onOpenSpecies = { id ->
            keyboard?.hide()
            onOpenSpecies(id)
        },
        onRetry = viewModel::onRetry,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PokedexContent(
    state: PokedexState,
    textFieldState: TextFieldState,
    onOpenSpecies: (Int) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchBarState = rememberSearchBarState()
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopSearchBar(
                state = searchBarState,
                scrollBehavior = scrollBehavior,
                inputField = {
                    SearchBarDefaults.InputField(
                        state = textFieldState,
                        onSearch = {},
                        expanded = false,
                        onExpandedChange = {},
                        modifier = Modifier.testTag("pokedex:search"),
                        placeholder = { Text(stringResource(R.string.pokedex_search_hint)) },
                        leadingIcon = {
                            Icon(painterResource(CoreUiR.drawable.core_ui_ic_search), contentDescription = null)
                        },
                        trailingIcon = {
                            if (textFieldState.text.isNotEmpty()) {
                                IconButton(onClick = { textFieldState.clearText() }, modifier = Modifier.testTag("pokedex:clear")) {
                                    Icon(
                                        painterResource(CoreUiR.drawable.core_ui_ic_close),
                                        contentDescription = stringResource(R.string.pokedex_clear_search),
                                    )
                                }
                            }
                        },
                    )
                },
            )
        },
    ) { innerPadding ->
        PokedexBody(state, innerPadding, onOpenSpecies, onRetry)
    }
}

@Composable
private fun PokedexBody(
    state: PokedexState,
    innerPadding: PaddingValues,
    onOpenSpecies: (Int) -> Unit,
    onRetry: () -> Unit,
) {
    val bodyModifier = Modifier.padding(innerPadding)
    val layoutDirection = LocalLayoutDirection.current
    when {
        state.loading -> LoadingState(bodyModifier)

        state.error != null -> MessageState(
            title = state.error.toMessage(),
            actionLabel = stringResource(CoreUiR.string.core_ui_retry),
            onAction = onRetry,
            modifier = bodyModifier,
        )

        state.results.isEmpty() -> MessageState(
            title = stringResource(R.string.pokedex_empty_title, state.query),
            message = stringResource(R.string.pokedex_empty_message),
            icon = ImageVector.vectorResource(CoreUiR.drawable.core_ui_ic_search),
            modifier = bodyModifier,
        )

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("pokedex:list"),
            contentPadding = PaddingValues(
                start = innerPadding.calculateStartPadding(layoutDirection),
                end = innerPadding.calculateEndPadding(layoutDirection),
                top = innerPadding.calculateTopPadding() + AppSpacing.xs,
                bottom = innerPadding.calculateBottomPadding() + AppSpacing.xxl + AppSpacing.xl,
            ),
        ) {
            items(state.results, key = { it.summary.id.value }, contentType = { "species" }) { result ->
                SpeciesRow(result, onClick = { onOpenSpecies(result.summary.id.value) }, Modifier.animateItem())
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SpeciesRow(
    result: SpeciesSearchResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = result.summary
    val number = speciesNumberLabel(summary.id.value)
    val typeNames = listText(summary.types.map { stringResource(it.labelRes()) })
    val description = stringResource(R.string.pokedex_row_description, number, summary.name, typeNames)
    val sharedScope = LocalSharedTransitionScope.current
    val artModifier = if (sharedScope != null) {
        val animatedScope = LocalNavAnimatedContentScope.current
        with(sharedScope) {
            Modifier.sharedBounds(rememberSharedContentState(key = "species-art-${summary.id.value}"), animatedScope)
        }
    } else {
        Modifier
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("pokedex:row:${summary.id.value}")
            .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm)
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        SpeciesArt(
            name = summary.name,
            types = summary.types,
            imagePath = null,
            contentDescription = null,
            letterStyle = MaterialTheme.typography.headlineSmall,
            modifier = artModifier.size(AppDimens.listArt),
        )
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Text(number, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(summary.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            result.matchedName?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TypeChipRow(summary.types)
        }
    }
}
