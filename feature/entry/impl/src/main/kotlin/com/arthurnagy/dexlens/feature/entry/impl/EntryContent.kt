package com.arthurnagy.dexlens.feature.entry.impl

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.arthurnagy.dexlens.core.designsystem.AppDimens
import com.arthurnagy.dexlens.core.designsystem.AppSpacing
import com.arthurnagy.dexlens.core.designsystem.MAX_BASE_STAT
import com.arthurnagy.dexlens.core.designsystem.StatBar
import com.arthurnagy.dexlens.core.designsystem.color
import com.arthurnagy.dexlens.core.designsystem.typeGradient
import com.arthurnagy.dexlens.core.model.Ability
import com.arthurnagy.dexlens.core.model.ArtworkLocation
import com.arthurnagy.dexlens.core.model.BaseStats
import com.arthurnagy.dexlens.core.model.EvolutionNode
import com.arthurnagy.dexlens.core.model.FlavorEntry
import com.arthurnagy.dexlens.core.model.GenderRatio
import com.arthurnagy.dexlens.core.model.LocalizedName
import com.arthurnagy.dexlens.core.model.PokemonType
import com.arthurnagy.dexlens.core.model.SpeciesDetail
import com.arthurnagy.dexlens.core.model.TypeEffectiveness
import com.arthurnagy.dexlens.core.model.TypeMatchups
import com.arthurnagy.dexlens.core.model.Variety
import com.arthurnagy.dexlens.core.ui.LoadingState
import com.arthurnagy.dexlens.core.ui.LocalSharedTransitionScope
import com.arthurnagy.dexlens.core.ui.MessageState
import com.arthurnagy.dexlens.core.ui.SpeciesArt
import com.arthurnagy.dexlens.core.ui.TypeChip
import com.arthurnagy.dexlens.core.ui.TypeChipRow
import com.arthurnagy.dexlens.core.ui.listText
import com.arthurnagy.dexlens.core.ui.speciesNumberLabel
import com.arthurnagy.dexlens.core.ui.toMessage
import com.arthurnagy.dexlens.core.ui.R as CoreUiR
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EntryContent(
    state: EntryState,
    onNavigateBack: (() -> Unit)?,
    onOpenSpecies: (Int) -> Unit,
    onSelectVariety: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("entry:back")) {
                            Icon(
                                painterResource(CoreUiR.drawable.core_ui_ic_arrow_back),
                                contentDescription = stringResource(CoreUiR.string.core_ui_navigate_back),
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        when (state) {
            EntryState.Loading -> LoadingState(Modifier.padding(innerPadding))

            is EntryState.Failed -> MessageState(
                title = state.error.toMessage(),
                actionLabel = onNavigateBack?.let { stringResource(R.string.entry_not_found_back) },
                onAction = onNavigateBack,
                modifier = Modifier.padding(innerPadding),
            )

            is EntryState.Loaded -> EntryBody(state, onOpenSpecies, onSelectVariety)
        }
    }
}

@Composable
private fun EntryBody(
    state: EntryState.Loaded,
    onOpenSpecies: (Int) -> Unit,
    onSelectVariety: (Int) -> Unit,
) {
    val detail = state.detail
    val variety = state.selected
    LazyColumn(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        ).testTag("entry:list"),
        contentPadding = PaddingValues(bottom = AppSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item(key = "header") { EntryHeader(detail, variety, state.artwork) }
        val contentModifier = Modifier.widthIn(max = AppDimens.maxContentWidth).fillMaxWidth().padding(horizontal = AppSpacing.md)
        if (detail.varieties.size > 1) {
            item(key = "varieties") {
                VarietyPicker(
                    detail.varieties,
                    variety.id,
                    onSelectVariety,
                    Modifier.widthIn(max = AppDimens.maxContentWidth).fillMaxWidth(),
                )
            }
        }
        item(key = "about") { EntrySection(stringResource(R.string.entry_about), contentModifier) { AboutSection(detail, variety) } }
        item(key = "stats") {
            EntrySection(stringResource(R.string.entry_base_stats), contentModifier) {
                StatsSection(variety.stats, variety.types.first().color)
            }
        }
        if (variety.abilities.isNotEmpty()) {
            item(key = "abilities") {
                EntrySection(stringResource(R.string.entry_abilities), contentModifier) { AbilitiesSection(variety.abilities) }
            }
        }
        item(key = "matchups") {
            EntrySection(stringResource(R.string.entry_matchups), contentModifier) { MatchupsSection(state.matchups) }
        }
        item(key = "evolution") {
            EntrySection(stringResource(R.string.entry_evolution), contentModifier) {
                EvolutionSection(detail, state.artwork, onOpenSpecies)
            }
        }
        if (state.flavor.isNotEmpty()) {
            item(key = "flavor") { EntrySection(stringResource(R.string.entry_flavor), contentModifier) { FlavorSection(state.flavor) } }
        }
        if (variety.forms.size > 1) {
            item(key = "forms") { EntrySection(stringResource(R.string.entry_forms), contentModifier) { FormsSection(variety) } }
        }
        if (detail.otherNames.isNotEmpty()) {
            item(key = "names") {
                EntrySection(stringResource(R.string.entry_other_names), contentModifier) { OtherNamesSection(detail.otherNames) }
            }
        }
        item(key = "bottom") { Spacer(Modifier.navigationBarsPadding()) }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun EntryHeader(
    detail: SpeciesDetail,
    variety: Variety,
    artwork: ArtworkLocation,
) {
    val summary = detail.summary
    val sharedScope = LocalSharedTransitionScope.current
    val artModifier = if (sharedScope != null) {
        val animatedScope = LocalNavAnimatedContentScope.current
        with(sharedScope) { Modifier.sharedBounds(rememberSharedContentState(key = "species-art-${summary.id.value}"), animatedScope) }
    } else {
        Modifier
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(typeGradient(variety.types) + MaterialTheme.colorScheme.surface))
            .statusBarsPadding()
            .padding(top = AppSpacing.xxl, bottom = AppSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        SpeciesArt(
            name = summary.name,
            types = summary.types,
            imagePath = artwork.artwork(summary.id),
            contentDescription = stringResource(R.string.entry_art_description, summary.name),
            letterStyle = MaterialTheme.typography.displayLarge,
            modifier = artModifier.size(AppDimens.headerArt),
        )
        Text(
            speciesNumberLabel(summary.id.value),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = variety.name.takeIf { !variety.isDefault } ?: summary.name,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() },
        )
        detail.genus?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        TypeChipRow(variety.types)
    }
}

@Composable
private fun VarietyPicker(
    varieties: List<Variety>,
    selectedId: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = AppSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        varieties.forEach { variety ->
            FilterChip(
                selected = variety.id == selectedId,
                onClick = { onSelect(variety.id) },
                label = { Text(variety.name) },
                modifier = Modifier.testTag("entry:variety:${variety.id}"),
            )
        }
    }
}

@Composable
private fun EntrySection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.padding(top = AppSpacing.lg), verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        content()
    }
}

@Composable
private fun AboutSection(
    detail: SpeciesDetail,
    variety: Variety,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        LabeledValue(
            stringResource(R.string.entry_height),
            stringResource(R.string.entry_height_value, decimal(variety.heightDecimetres / DECIMETRES_PER_METRE)),
        )
        LabeledValue(
            stringResource(R.string.entry_weight),
            stringResource(R.string.entry_weight_value, decimal(variety.weightHectograms / HECTOGRAMS_PER_KILOGRAM)),
        )
        LabeledValue(stringResource(R.string.entry_generation), stringResource(R.string.entry_generation_value, detail.generation))
        LabeledValue(stringResource(R.string.entry_egg_groups), listText(detail.eggGroups))
        GenderRow(detail.genderRatio)
    }
}

@Composable
private fun LabeledValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun GenderRow(ratio: GenderRatio) {
    val female = ratio.femaleEighths
    if (female == null) {
        LabeledValue(stringResource(R.string.entry_gender), stringResource(R.string.entry_genderless))
    } else {
        val femalePercent = female * PERCENT_PER_EIGHTH
        val malePercent = FULL_PERCENT - femalePercent
        val text = stringResource(R.string.entry_gender_value, percent(malePercent), percent(femalePercent))
        Column(
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
            modifier = Modifier.clearAndSetSemantics {
                contentDescription =
                    text
            },
        ) {
            LabeledValue(stringResource(R.string.entry_gender), text)
            Row(
                Modifier.fillMaxWidth().height(
                    AppDimens.genderBarHeight,
                ).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                if (nonZero(
                        malePercent,
                    )
                ) {
                    Box(Modifier.weight(malePercent.toFloat()).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                }
                if (nonZero(
                        femalePercent,
                    )
                ) {
                    Box(Modifier.weight(femalePercent.toFloat()).fillMaxHeight().background(MaterialTheme.colorScheme.tertiary))
                }
            }
        }
    }
}

private fun nonZero(value: Double) = value > 0.0

@Composable
private fun StatsSection(
    stats: BaseStats,
    color: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        StatRow(stringResource(R.string.entry_stat_hp), stats.hp, color)
        StatRow(stringResource(R.string.entry_stat_attack), stats.attack, color)
        StatRow(stringResource(R.string.entry_stat_defense), stats.defense, color)
        StatRow(stringResource(R.string.entry_stat_special_attack), stats.specialAttack, color)
        StatRow(stringResource(R.string.entry_stat_special_defense), stats.specialDefense, color)
        StatRow(stringResource(R.string.entry_stat_speed), stats.speed, color)
        LabeledValue(stringResource(R.string.entry_stat_total), stats.total.toString())
    }
}

@Composable
private fun StatRow(
    label: String,
    value: Int,
    color: Color,
) {
    StatBar(
        label = label,
        value = value,
        color = color,
        description = stringResource(R.string.entry_stat_description, label, value, MAX_BASE_STAT),
    )
}

@Composable
private fun AbilitiesSection(abilities: List<Ability>) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        abilities.forEach { ability -> AbilityCard(ability) }
    }
}

@Composable
private fun AbilityCard(ability: Ability) {
    var expanded by rememberSaveable(ability.name) { mutableStateOf(false) }
    val hasLongText = !ability.effect.isNullOrBlank() && ability.effect != ability.shortEffect
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(AppDimens.cardCorner),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.cardCorner))
            .then(
                if (hasLongText) {
                    Modifier.clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(if (expanded) R.string.entry_ability_collapse else R.string.entry_ability_expand),
                    ) {
                        expanded = !expanded
                    }
                } else {
                    Modifier
                },
            )
            .animateContentSize(),
    ) {
        Column(Modifier.padding(AppSpacing.md), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                Text(ability.name, style = MaterialTheme.typography.titleMedium)
                if (ability.isHidden) {
                    Text(
                        stringResource(R.string.entry_ability_hidden),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                text = if (expanded) ability.effect.orEmpty() else ability.shortEffect.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MatchupsSection(matchups: TypeMatchups) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
        if (matchups.weaknesses.isEmpty() && matchups.resistances.isEmpty() && matchups.immunities.isEmpty()) {
            Text(stringResource(R.string.entry_matchups_none), style = MaterialTheme.typography.bodyMedium)
        }
        MatchupGroup(stringResource(R.string.entry_weak_to), matchups.weaknesses)
        MatchupGroup(stringResource(R.string.entry_resists), matchups.resistances)
        if (matchups.immunities.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Text(stringResource(R.string.entry_immune_to), style = MaterialTheme.typography.labelLarge)
                TypeChipRow(matchups.immunities)
            }
        }
    }
}

@Composable
private fun MatchupGroup(
    title: String,
    entries: List<TypeEffectiveness>,
) {
    if (entries.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            entries.forEach { entry -> TypeChip(entry.type, trailingText = multiplierText(entry.multiplier)) }
        }
    }
}

@Composable
private fun multiplierText(multiplier: Double): String = when {
    multiplier >= FOUR_TIMES -> stringResource(R.string.entry_multiplier_four)
    multiplier >= TWO_TIMES -> stringResource(R.string.entry_multiplier_two)
    multiplier >= HALF -> stringResource(R.string.entry_multiplier_half)
    else -> stringResource(R.string.entry_multiplier_quarter)
}

@Composable
private fun EvolutionSection(
    detail: SpeciesDetail,
    artwork: ArtworkLocation,
    onOpenSpecies: (Int) -> Unit,
) {
    val root = detail.evolutionLine
    if (root.children.isEmpty()) {
        Text(
            stringResource(R.string.entry_evolution_none),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            EvolutionTree(root, detail.summary.id.value, depth = 0, artwork, onOpenSpecies)
        }
    }
}

@Composable
private fun EvolutionTree(
    node: EvolutionNode,
    currentId: Int,
    depth: Int,
    artwork: ArtworkLocation,
    onOpenSpecies: (Int) -> Unit,
) {
    EvolutionRow(node, currentId, AppDimens.treeIndent * depth, artwork, onOpenSpecies)
    node.children.forEach { child -> EvolutionTree(child, currentId, depth + 1, artwork, onOpenSpecies) }
}

@Composable
private fun EvolutionRow(
    node: EvolutionNode,
    currentId: Int,
    indent: Dp,
    artwork: ArtworkLocation,
    onOpenSpecies: (Int) -> Unit,
) {
    val id = node.species.id.value
    val isCurrent = id == currentId
    val resources = LocalResources.current
    val methodText = remember(node.methods, resources) {
        node.methods
            .takeIf { it.isNotEmpty() }
            ?.map { evolutionMethodText(it, resources) }
            ?.distinct()
            ?.joinToString(separator = "\n")
    }
    val description = listText(
        listOfNotNull(
            if (isCurrent) stringResource(R.string.entry_evolution_current, node.species.name) else node.species.name,
            methodText,
        ),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.cardCorner))
            .then(
                if (isCurrent) {
                    Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)
                } else {
                    Modifier.clickable {
                        onOpenSpecies(id)
                    }
                },
            )
            .testTag("entry:evolution:$id")
            .padding(start = AppSpacing.sm + indent, top = AppSpacing.sm, bottom = AppSpacing.sm, end = AppSpacing.sm)
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
        SpeciesArt(
            name = node.species.name,
            types = node.species.types,
            imagePath = artwork.artwork(node.species.id),
            contentDescription = null,
            letterStyle = MaterialTheme.typography.titleLarge,
            modifier = Modifier.size(AppDimens.evolutionArt),
        )
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
            Text(node.species.name, style = MaterialTheme.typography.titleMedium)
            methodText?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun FlavorSection(entries: List<FlavorEntry>) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
        entries.forEach { entry ->
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Text(entry.text, style = MaterialTheme.typography.bodyMedium)
                Text(
                    listText(entry.games),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FormsSection(variety: Variety) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        variety.forms.forEach { form ->
            Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = CircleShape) {
                Text(
                    form.name,
                    Modifier.padding(horizontal = AppSpacing.sm + AppSpacing.xs, vertical = AppSpacing.xs),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun OtherNamesSection(names: List<LocalizedName>) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
        names.forEachIndexed { index, name ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = DIVIDER_ALPHA))
            LabeledValue(name.language, name.name, Modifier.padding(vertical = AppSpacing.xs))
        }
    }
}

private fun decimal(value: Double): String = "%.1f".format(Locale.getDefault(), value)

private fun percent(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(Locale.getDefault(), value)

private const val DECIMETRES_PER_METRE = 10.0
private const val HECTOGRAMS_PER_KILOGRAM = 10.0
private const val PERCENT_PER_EIGHTH = 12.5
private const val FULL_PERCENT = 100.0
private const val FOUR_TIMES = 4.0
private const val TWO_TIMES = 2.0
private const val HALF = 0.5
private const val DIVIDER_ALPHA = 0.5f
