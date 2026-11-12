package com.arthurnagy.dexlens.feature.entry.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.arthurnagy.dexlens.core.designsystem.AppDimens
import com.arthurnagy.dexlens.core.designsystem.AppSpacing
import com.arthurnagy.dexlens.core.model.Game
import com.arthurnagy.dexlens.core.model.LevelUpMove
import com.arthurnagy.dexlens.core.model.WildEncounter
import com.arthurnagy.dexlens.core.ui.TypeChip
import com.arthurnagy.dexlens.core.ui.labelRes
import com.arthurnagy.dexlens.core.ui.listText

/** The "In game" section: a game picker, then that game's level-up moves and wild locations, one lazy item per row. */
internal fun LazyListScope.inGameItems(
    inGame: InGameState,
    onSelectGame: (Int) -> Unit,
    modifier: Modifier,
) {
    val details = inGame.details
    item(key = "in-game", contentType = "section") {
        EntrySection(stringResource(R.string.entry_in_game), modifier) { GamePicker(inGame.games, inGame.selected, onSelectGame) }
    }
    item(key = "moves-title", contentType = "subheading") { Subheading(stringResource(R.string.entry_moves_level_up), modifier) }
    if (details.moves.isEmpty()) {
        item(key = "moves-none", contentType = "note") { Note(stringResource(R.string.entry_moves_none), modifier) }
    } else {
        item(key = "moves-header", contentType = "move-header") { MoveHeader(modifier) }
        itemsIndexed(details.moves, key = { index, _ -> "move-$index" }, contentType = { _, _ -> "move" }) { index, move ->
            MoveRow(move, modifier.testTag("entry:move:$index"))
        }
    }
    item(key = "locations-title", contentType = "subheading") { Subheading(stringResource(R.string.entry_locations), modifier) }
    when {
        !details.game.hasLocationData -> item(key = "locations-unavailable", contentType = "note") {
            Note(stringResource(R.string.entry_locations_unavailable), modifier.testTag("entry:locations-unavailable"))
        }

        details.encounters.isEmpty() -> item(key = "locations-none", contentType = "note") {
            Note(stringResource(R.string.entry_locations_none), modifier.testTag("entry:locations-none"))
        }

        else -> itemsIndexed(details.encounters, key = { index, _ ->
            "location-$index"
        }, contentType = { _, _ -> "location" }) { index, encounter ->
            EncounterRow(encounter, modifier.testTag("entry:location:$index"))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GamePicker(
    games: List<Game>,
    selected: Game,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = gameName(selected),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.entry_game)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .testTag("entry:game-picker"),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            games.forEach { game ->
                DropdownMenuItem(
                    text = { Text(gameName(game)) },
                    onClick = {
                        expanded = false
                        onSelect(game.id)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    modifier = Modifier.testTag("entry:game:${game.id}"),
                )
            }
        }
    }
}

@Composable
private fun Subheading(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.padding(top = AppSpacing.lg, bottom = AppSpacing.xs).semantics { heading() },
    )
}

@Composable
private fun Note(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

/** Column names for sighted readers; each row speaks its own labels. */
@Composable
private fun MoveHeader(modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.labelMedium
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier = modifier.clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        Text(
            stringResource(R.string.entry_move_column_level),
            style = style,
            color = color,
            modifier = Modifier.width(AppDimens.moveLevelWidth),
        )
        Text(stringResource(R.string.entry_move_column_move), style = style, color = color, modifier = Modifier.weight(1f))
        Text(
            stringResource(R.string.entry_move_column_type),
            style = style,
            color = color,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(AppDimens.moveTypeWidth),
        )
        Text(
            stringResource(R.string.entry_move_column_power),
            style = style,
            color = color,
            textAlign = TextAlign.End,
            modifier = Modifier.width(AppDimens.movePowerWidth),
        )
    }
}

@Composable
private fun MoveRow(
    move: LevelUpMove,
    modifier: Modifier = Modifier,
) {
    val evolves = move.level == 0
    val typeLabel = move.type?.let { stringResource(it.labelRes()) }
    val description = listText(
        listOfNotNull(
            if (evolves) {
                stringResource(
                    R.string.entry_move_evolve_spoken,
                )
            } else {
                stringResource(R.string.entry_move_level_spoken, move.level)
            },
            move.name,
            typeLabel,
            move.power?.let { stringResource(R.string.entry_move_power_spoken, it) } ?: stringResource(R.string.entry_move_no_power_spoken),
        ),
    )
    Row(
        modifier = modifier
            .padding(vertical = AppSpacing.xs)
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    ) {
        Text(
            if (evolves) stringResource(R.string.entry_move_evolve) else stringResource(R.string.entry_move_level, move.level),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(AppDimens.moveLevelWidth),
        )
        Text(move.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Box(Modifier.width(AppDimens.moveTypeWidth), contentAlignment = Alignment.Center) { move.type?.let { TypeChip(it) } }
        Text(
            move.power?.let { stringResource(R.string.entry_move_power, it) } ?: stringResource(R.string.entry_move_no_power),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.End,
            modifier = Modifier.width(AppDimens.movePowerWidth),
        )
    }
}

@Composable
private fun EncounterRow(
    encounter: WildEncounter,
    modifier: Modifier = Modifier,
) {
    val levels = if (encounter.minLevel == encounter.maxLevel) {
        stringResource(R.string.entry_location_level, encounter.minLevel)
    } else {
        stringResource(R.string.entry_location_levels, encounter.minLevel, encounter.maxLevel)
    }
    val onlyIn = encounter.onlyIn.takeIf { it.isNotEmpty() }?.let { stringResource(R.string.entry_location_only_in, listText(it)) }
    val details = listText(listOfNotNull(methodText(encounter.method), levels, onlyIn))
    val description = listText(listOf(encounter.location, details))
    Column(
        modifier = modifier
            .padding(vertical = AppSpacing.xs)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Text(encounter.location, style = MaterialTheme.typography.bodyLarge)
        Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
