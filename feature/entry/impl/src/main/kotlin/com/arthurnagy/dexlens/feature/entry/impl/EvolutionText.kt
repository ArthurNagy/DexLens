package com.arthurnagy.dexlens.feature.entry.impl

import android.content.res.Resources
import com.arthurnagy.dexlens.core.model.EvolutionMethod
import com.arthurnagy.dexlens.core.model.EvolutionTrigger
import com.arthurnagy.dexlens.core.model.Gender
import com.arthurnagy.dexlens.core.model.RelativePhysicalStats
import com.arthurnagy.dexlens.core.model.TimeOfDay
import com.arthurnagy.dexlens.core.ui.labelRes
import com.arthurnagy.dexlens.core.ui.R as CoreUiR

/** One evolution method in words: the trigger, then each condition. */
internal fun evolutionMethodText(
    method: EvolutionMethod,
    resources: Resources,
): String = (listOf(triggerText(method, resources)) + conditionTexts(method, resources))
    .joinToString(resources.getString(CoreUiR.string.core_ui_list_separator))

private fun triggerText(
    method: EvolutionMethod,
    resources: Resources,
): String = when (method.trigger) {
    EvolutionTrigger.LEVEL_UP -> method.minLevel?.let { resources.getString(R.string.entry_evo_level, it) }
        ?: resources.getString(R.string.entry_evo_level_up)

    EvolutionTrigger.TRADE -> method.heldItem?.let { resources.getString(R.string.entry_evo_trade_holding, it) }
        ?: resources.getString(R.string.entry_evo_trade)

    EvolutionTrigger.USE_ITEM -> resources.getString(R.string.entry_evo_use_item, method.item.orEmpty())

    EvolutionTrigger.SHED -> resources.getString(R.string.entry_evo_shed)

    EvolutionTrigger.SPIN -> resources.getString(R.string.entry_evo_spin)

    EvolutionTrigger.TOWER_OF_DARKNESS -> resources.getString(R.string.entry_evo_tower_darkness)

    EvolutionTrigger.TOWER_OF_WATERS -> resources.getString(R.string.entry_evo_tower_waters)

    EvolutionTrigger.THREE_CRITICAL_HITS -> resources.getString(R.string.entry_evo_critical_hits)

    EvolutionTrigger.TAKE_DAMAGE -> (method.minDamageTaken ?: 0).let {
        resources.getQuantityString(R.plurals.entry_evo_take_damage, it, it)
    }

    EvolutionTrigger.IN_BATTLE_LEVEL_UP -> resources.getString(R.string.entry_evo_in_battle_level)

    EvolutionTrigger.AGILE_STYLE_MOVE -> resources.getString(R.string.entry_evo_agile_style, method.usedMove.orEmpty())

    EvolutionTrigger.STRONG_STYLE_MOVE -> resources.getString(R.string.entry_evo_strong_style, method.usedMove.orEmpty())

    EvolutionTrigger.RECOIL_DAMAGE -> (method.minDamageTaken ?: 0).let { resources.getQuantityString(R.plurals.entry_evo_recoil, it, it) }

    EvolutionTrigger.USE_MOVE -> (method.minMoveCount ?: 1).let {
        resources.getQuantityString(R.plurals.entry_evo_use_move, it, it, method.usedMove.orEmpty())
    }

    EvolutionTrigger.THREE_DEFEATED_BISHARP -> resources.getString(R.string.entry_evo_bisharp)

    EvolutionTrigger.GIMMIGHOUL_COINS -> resources.getString(R.string.entry_evo_coins)

    EvolutionTrigger.MELTAN_CANDIES -> resources.getString(R.string.entry_evo_candies)

    EvolutionTrigger.OTHER -> resources.getString(R.string.entry_evo_other)
}

private fun conditionTexts(
    method: EvolutionMethod,
    resources: Resources,
): List<String> = buildList {
    if (method.trigger != EvolutionTrigger.TRADE) method.heldItem?.let { add(resources.getString(R.string.entry_evo_holding, it)) }
    method.timeOfDay?.let {
        add(
            resources.getString(
                when (it) {
                    TimeOfDay.DAY -> R.string.entry_evo_day
                    TimeOfDay.NIGHT -> R.string.entry_evo_night
                    TimeOfDay.DUSK -> R.string.entry_evo_dusk
                },
            ),
        )
    }
    method.knownMove?.let { add(resources.getString(R.string.entry_evo_knowing_move, it)) }
    method.knownMoveType?.let { add(resources.getString(R.string.entry_evo_knowing_type, resources.getString(it.labelRes()))) }
    if (method.minHappiness != null) add(resources.getString(R.string.entry_evo_friendship))
    if (method.minBeauty != null) add(resources.getString(R.string.entry_evo_beauty))
    if (method.minAffection != null) add(resources.getString(R.string.entry_evo_affection))
    method.relativePhysicalStats?.let {
        add(
            resources.getString(
                when (it) {
                    RelativePhysicalStats.ATTACK_HIGHER -> R.string.entry_evo_attack_higher
                    RelativePhysicalStats.DEFENSE_HIGHER -> R.string.entry_evo_defense_higher
                    RelativePhysicalStats.EQUAL -> R.string.entry_evo_stats_equal
                },
            ),
        )
    }
    method.partySpecies?.let { add(resources.getString(R.string.entry_evo_party_species, it)) }
    method.partyType?.let { add(resources.getString(R.string.entry_evo_party_type, resources.getString(it.labelRes()))) }
    method.tradeSpecies?.let { add(resources.getString(R.string.entry_evo_trade_for, it)) }
    method.location?.let { add(resources.getString(R.string.entry_evo_location, it)) }
    method.region?.let { add(resources.getString(R.string.entry_evo_region, it)) }
    method.gender?.let { add(resources.getString(if (it == Gender.FEMALE) R.string.entry_evo_female else R.string.entry_evo_male)) }
    if (method.needsRain) add(resources.getString(R.string.entry_evo_rain))
    if (method.turnUpsideDown) add(resources.getString(R.string.entry_evo_upside_down))
    method.minSteps?.let { add(resources.getQuantityString(R.plurals.entry_evo_steps, it, it)) }
}
