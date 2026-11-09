package com.arthurnagy.dexlens.feature.entry.impl

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.arthurnagy.dexlens.core.model.EvolutionMethod
import com.arthurnagy.dexlens.core.model.EvolutionTrigger
import com.arthurnagy.dexlens.core.model.Gender
import com.arthurnagy.dexlens.core.model.RelativePhysicalStats
import com.arthurnagy.dexlens.core.model.TimeOfDay
import com.arthurnagy.dexlens.core.ui.labelRes
import com.arthurnagy.dexlens.core.ui.listText

/** One evolution method in words, built from string resources: the trigger, then each condition. */
@Composable
internal fun evolutionMethodText(method: EvolutionMethod): String {
    val parts = buildList {
        add(triggerText(method))
        addAll(conditionTexts(method))
    }
    return listText(parts)
}

@Composable
private fun triggerText(method: EvolutionMethod): String = when (method.trigger) {
    EvolutionTrigger.LEVEL_UP -> method.minLevel?.let { stringResource(R.string.entry_evo_level, it) }
        ?: stringResource(R.string.entry_evo_level_up)

    EvolutionTrigger.TRADE -> method.heldItem?.let { stringResource(R.string.entry_evo_trade_holding, it) }
        ?: stringResource(R.string.entry_evo_trade)

    EvolutionTrigger.USE_ITEM -> stringResource(R.string.entry_evo_use_item, method.item.orEmpty())

    EvolutionTrigger.SHED -> stringResource(R.string.entry_evo_shed)

    EvolutionTrigger.SPIN -> stringResource(R.string.entry_evo_spin)

    EvolutionTrigger.TOWER_OF_DARKNESS -> stringResource(R.string.entry_evo_tower_darkness)

    EvolutionTrigger.TOWER_OF_WATERS -> stringResource(R.string.entry_evo_tower_waters)

    EvolutionTrigger.THREE_CRITICAL_HITS -> stringResource(R.string.entry_evo_critical_hits)

    EvolutionTrigger.TAKE_DAMAGE -> (method.minDamageTaken ?: 0).let { pluralStringResource(R.plurals.entry_evo_take_damage, it, it) }

    EvolutionTrigger.IN_BATTLE_LEVEL_UP -> stringResource(R.string.entry_evo_in_battle_level)

    EvolutionTrigger.AGILE_STYLE_MOVE -> stringResource(R.string.entry_evo_agile_style, method.usedMove.orEmpty())

    EvolutionTrigger.STRONG_STYLE_MOVE -> stringResource(R.string.entry_evo_strong_style, method.usedMove.orEmpty())

    EvolutionTrigger.RECOIL_DAMAGE -> (method.minDamageTaken ?: 0).let { pluralStringResource(R.plurals.entry_evo_recoil, it, it) }

    EvolutionTrigger.USE_MOVE -> (method.minMoveCount ?: 1).let {
        pluralStringResource(R.plurals.entry_evo_use_move, it, it, method.usedMove.orEmpty())
    }

    EvolutionTrigger.THREE_DEFEATED_BISHARP -> stringResource(R.string.entry_evo_bisharp)

    EvolutionTrigger.GIMMIGHOUL_COINS -> stringResource(R.string.entry_evo_coins)

    EvolutionTrigger.MELTAN_CANDIES -> stringResource(R.string.entry_evo_candies)

    EvolutionTrigger.OTHER -> stringResource(R.string.entry_evo_other)
}

@Composable
private fun conditionTexts(method: EvolutionMethod): List<String> = buildList {
    if (method.trigger != EvolutionTrigger.TRADE) method.heldItem?.let { add(stringResource(R.string.entry_evo_holding, it)) }
    method.timeOfDay?.let {
        add(
            stringResource(
                when (it) {
                    TimeOfDay.DAY -> R.string.entry_evo_day
                    TimeOfDay.NIGHT -> R.string.entry_evo_night
                    TimeOfDay.DUSK -> R.string.entry_evo_dusk
                },
            ),
        )
    }
    method.knownMove?.let { add(stringResource(R.string.entry_evo_knowing_move, it)) }
    method.knownMoveType?.let { add(stringResource(R.string.entry_evo_knowing_type, stringResource(it.labelRes()))) }
    if (method.minHappiness != null) add(stringResource(R.string.entry_evo_friendship))
    if (method.minBeauty != null) add(stringResource(R.string.entry_evo_beauty))
    if (method.minAffection != null) add(stringResource(R.string.entry_evo_affection))
    method.relativePhysicalStats?.let {
        add(
            stringResource(
                when (it) {
                    RelativePhysicalStats.ATTACK_HIGHER -> R.string.entry_evo_attack_higher
                    RelativePhysicalStats.DEFENSE_HIGHER -> R.string.entry_evo_defense_higher
                    RelativePhysicalStats.EQUAL -> R.string.entry_evo_stats_equal
                },
            ),
        )
    }
    method.partySpecies?.let { add(stringResource(R.string.entry_evo_party_species, it)) }
    method.partyType?.let { add(stringResource(R.string.entry_evo_party_type, stringResource(it.labelRes()))) }
    method.tradeSpecies?.let { add(stringResource(R.string.entry_evo_trade_for, it)) }
    method.location?.let { add(stringResource(R.string.entry_evo_location, it)) }
    method.region?.let { add(stringResource(R.string.entry_evo_region, it)) }
    method.gender?.let { add(stringResource(if (it == Gender.FEMALE) R.string.entry_evo_female else R.string.entry_evo_male)) }
    if (method.needsRain) add(stringResource(R.string.entry_evo_rain))
    if (method.turnUpsideDown) add(stringResource(R.string.entry_evo_upside_down))
    method.minSteps?.let { add(pluralStringResource(R.plurals.entry_evo_steps, it, it)) }
}
