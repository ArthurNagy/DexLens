package com.arthurnagy.dexlens.feature.entry.impl

import org.junit.Test
import kotlin.test.assertEquals

/** Every way of finding a Pokémon in the bundled database is worded as a string, except a few one-off events. */
class InGameTextTest {
    private val bundled =
        listOf(
            "berry-trees",
            "bridge-spots",
            "bubbling-spots",
            "cave-spots",
            "ceiling-ambush",
            "chase-water",
            "colosseum-bonus-disc-jpn",
            "colosseum-bonus-disc-us",
            "dark-grass",
            "devon-scope",
            "dynamax-adventure",
            "feebas-tile-fishing",
            "gift",
            "gift-egg",
            "good-rod",
            "grass-spots",
            "ground-ambush",
            "headbutt",
            "hidden-grotto",
            "honey-tree",
            "horde",
            "island-scan",
            "max-raid",
            "npc-trade",
            "old-rod",
            "overworld",
            "overworld-dirt",
            "overworld-flying",
            "overworld-flying-special",
            "overworld-special",
            "overworld-water",
            "overworld-water-special",
            "pokeflute",
            "pokemon-channel-pal",
            "pokemon-ranger",
            "pokespot",
            "purple-flowers",
            "red-flowers",
            "roaming-grass",
            "roaming-water",
            "rock-smash",
            "rough-terrain",
            "rustling-bush-ambush",
            "seaweed",
            "sky-ambush",
            "snag",
            "sos",
            "sos-from-bubbling-spot",
            "squirt-bottle",
            "static",
            "super-rod",
            "super-rod-spots",
            "surf",
            "surf-spots",
            "trash-can-ambush",
            "wailmer-pail",
            "walk",
            "wanderer",
            "wanderer-water",
            "yellow-flowers",
        )
    private val oneOffEvents = setOf("colosseum-bonus-disc-jpn", "colosseum-bonus-disc-us", "pokemon-channel-pal", "pokemon-ranger")

    @Test
    fun everyBundledMethodHasAStringOrIsAOneOffEvent() {
        assertEquals(oneOffEvents, bundled.filter { methodRes(it) == null }.toSet())
    }
}
