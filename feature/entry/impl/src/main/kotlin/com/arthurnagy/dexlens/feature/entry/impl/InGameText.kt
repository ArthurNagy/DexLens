package com.arthurnagy.dexlens.feature.entry.impl

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.arthurnagy.dexlens.core.model.EncounterMethod
import com.arthurnagy.dexlens.core.model.Game

/** A game pair's name, such as "Red / Blue". */
@Composable
internal fun gameName(game: Game): String = game.versions.joinToString(stringResource(R.string.entry_game_separator))

/** How a Pokémon is found, in the display language, or PokeAPI's English description for a method without a string. */
@Composable
internal fun methodText(method: EncounterMethod): String = methodRes(method.identifier)?.let { stringResource(it) } ?: method.name

@StringRes
internal fun methodRes(identifier: String): Int? = when (identifier) {
    "berry-trees" -> R.string.entry_method_berry_trees
    "bridge-spots" -> R.string.entry_method_bridge_spots
    "bubbling-spots" -> R.string.entry_method_bubbling_spots
    "cave-spots" -> R.string.entry_method_cave_spots
    "ceiling-ambush" -> R.string.entry_method_ceiling_ambush
    "chase-water" -> R.string.entry_method_chase_water
    "dark-grass" -> R.string.entry_method_dark_grass
    "devon-scope" -> R.string.entry_method_devon_scope
    "dynamax-adventure" -> R.string.entry_method_dynamax_adventure
    "feebas-tile-fishing" -> R.string.entry_method_feebas_tile_fishing
    "gift" -> R.string.entry_method_gift
    "gift-egg" -> R.string.entry_method_gift_egg
    "good-rod" -> R.string.entry_method_good_rod
    "grass-spots" -> R.string.entry_method_grass_spots
    "ground-ambush" -> R.string.entry_method_ground_ambush
    "headbutt" -> R.string.entry_method_headbutt
    "hidden-grotto" -> R.string.entry_method_hidden_grotto
    "honey-tree" -> R.string.entry_method_honey_tree
    "horde" -> R.string.entry_method_horde
    "island-scan" -> R.string.entry_method_island_scan
    "max-raid" -> R.string.entry_method_max_raid
    "npc-trade" -> R.string.entry_method_npc_trade
    "old-rod" -> R.string.entry_method_old_rod
    "overworld" -> R.string.entry_method_overworld
    "overworld-dirt" -> R.string.entry_method_overworld_dirt
    "overworld-flying" -> R.string.entry_method_overworld_flying
    "overworld-flying-special" -> R.string.entry_method_overworld_flying_special
    "overworld-special" -> R.string.entry_method_overworld_special
    "overworld-water" -> R.string.entry_method_overworld_water
    "overworld-water-special" -> R.string.entry_method_overworld_water_special
    "pokeflute" -> R.string.entry_method_pokeflute
    "pokespot" -> R.string.entry_method_pokespot
    "purple-flowers" -> R.string.entry_method_purple_flowers
    "red-flowers" -> R.string.entry_method_red_flowers
    "roaming-grass" -> R.string.entry_method_roaming_grass
    "roaming-water" -> R.string.entry_method_roaming_water
    "rock-smash" -> R.string.entry_method_rock_smash
    "rough-terrain" -> R.string.entry_method_rough_terrain
    "rustling-bush-ambush" -> R.string.entry_method_rustling_bush_ambush
    "seaweed" -> R.string.entry_method_seaweed
    "sky-ambush" -> R.string.entry_method_sky_ambush
    "snag" -> R.string.entry_method_snag
    "sos" -> R.string.entry_method_sos
    "sos-from-bubbling-spot" -> R.string.entry_method_sos_from_bubbling_spot
    "squirt-bottle" -> R.string.entry_method_squirt_bottle
    "static" -> R.string.entry_method_static
    "super-rod" -> R.string.entry_method_super_rod
    "super-rod-spots" -> R.string.entry_method_super_rod_spots
    "surf" -> R.string.entry_method_surf
    "surf-spots" -> R.string.entry_method_surf_spots
    "trash-can-ambush" -> R.string.entry_method_trash_can_ambush
    "wailmer-pail" -> R.string.entry_method_wailmer_pail
    "walk" -> R.string.entry_method_walk
    "wanderer" -> R.string.entry_method_wanderer
    "wanderer-water" -> R.string.entry_method_wanderer_water
    "yellow-flowers" -> R.string.entry_method_yellow_flowers
    else -> null
}
