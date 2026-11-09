package com.arthurnagy.dexlens.core.database

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Fts5
import androidx.room3.FtsOptions
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** One species. [id] is the National Pokédex number. */
@Entity(tableName = "species")
data class SpeciesEntity(
    @PrimaryKey val id: Int,
    val identifier: String,
    val generation: Int,
    /** Eighths of the population that are female, or -1 when the species has no gender. */
    @ColumnInfo(name = "gender_rate") val genderRate: Int,
    @ColumnInfo(name = "evolves_from_species_id") val evolvesFromSpeciesId: Int?,
    @ColumnInfo(name = "evolution_chain_id", index = true) val evolutionChainId: Int,
    @ColumnInfo(name = "is_baby") val isBaby: Boolean,
    @ColumnInfo(name = "is_legendary") val isLegendary: Boolean,
    @ColumnInfo(name = "is_mythical") val isMythical: Boolean,
)

@Entity(tableName = "species_name", primaryKeys = ["species_id", "language_id"], indices = [Index("language_id")])
data class SpeciesNameEntity(
    @ColumnInfo(name = "species_id") val speciesId: Int,
    @ColumnInfo(name = "language_id") val languageId: Int,
    val name: String,
    val genus: String?,
)

@Entity(tableName = "language")
data class LanguageEntity(
    @PrimaryKey val id: Int,
    val identifier: String,
    val name: String,
)

/** A `pokemon` row: a battle-distinct variety of a species, with its own types, stats and abilities. */
@Entity(tableName = "variety", indices = [Index("species_id")])
data class VarietyEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "species_id") val speciesId: Int,
    val identifier: String,
    val name: String,
    @ColumnInfo(name = "is_default") val isDefault: Boolean,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    /** Decimetres. */
    val height: Int,
    /** Hectograms. */
    val weight: Int,
    @ColumnInfo(name = "type_primary") val typePrimary: Int,
    @ColumnInfo(name = "type_secondary") val typeSecondary: Int?,
    val hp: Int,
    val attack: Int,
    val defense: Int,
    @ColumnInfo(name = "special_attack") val specialAttack: Int,
    @ColumnInfo(name = "special_defense") val specialDefense: Int,
    val speed: Int,
)

/** A `pokemon_forms` row: a visual or battle-only form of a variety. */
@Entity(tableName = "form", indices = [Index("variety_id")])
data class FormEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "variety_id") val varietyId: Int,
    val identifier: String,
    val name: String,
    @ColumnInfo(name = "is_default") val isDefault: Boolean,
    @ColumnInfo(name = "is_battle_only") val isBattleOnly: Boolean,
    @ColumnInfo(name = "is_mega") val isMega: Boolean,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    @ColumnInfo(name = "type_primary") val typePrimary: Int?,
    @ColumnInfo(name = "type_secondary") val typeSecondary: Int?,
)

/** PokeAPI's own wording ("ability_prose"), never in-game text. */
@Entity(tableName = "ability")
data class AbilityEntity(
    @PrimaryKey val id: Int,
    val identifier: String,
    val name: String,
    @ColumnInfo(name = "short_effect") val shortEffect: String?,
    val effect: String?,
)

@Entity(tableName = "variety_ability", primaryKeys = ["variety_id", "slot"], indices = [Index("ability_id")])
internal data class VarietyAbilityEntity(
    @ColumnInfo(name = "variety_id") val varietyId: Int,
    val slot: Int,
    @ColumnInfo(name = "ability_id") val abilityId: Int,
    @ColumnInfo(name = "is_hidden") val isHidden: Boolean,
)

@Entity(tableName = "egg_group")
data class EggGroupEntity(
    @PrimaryKey val id: Int,
    val identifier: String,
    val name: String,
)

@Entity(tableName = "species_egg_group", primaryKeys = ["species_id", "egg_group_id"], indices = [Index("egg_group_id")])
internal data class SpeciesEggGroupEntity(
    @ColumnInfo(name = "species_id") val speciesId: Int,
    @ColumnInfo(name = "egg_group_id") val eggGroupId: Int,
)

/** How [fromSpeciesId] evolves into [speciesId]. Secondary names are English, the UI's language. */
@Entity(tableName = "evolution", indices = [Index("species_id"), Index("from_species_id")])
data class EvolutionEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "species_id") val speciesId: Int,
    @ColumnInfo(name = "from_species_id") val fromSpeciesId: Int,
    val trigger: String,
    @ColumnInfo(name = "min_level") val minLevel: Int?,
    val item: String?,
    @ColumnInfo(name = "held_item") val heldItem: String?,
    @ColumnInfo(name = "time_of_day") val timeOfDay: String?,
    @ColumnInfo(name = "known_move") val knownMove: String?,
    @ColumnInfo(name = "known_move_type") val knownMoveType: Int?,
    @ColumnInfo(name = "min_happiness") val minHappiness: Int?,
    @ColumnInfo(name = "min_beauty") val minBeauty: Int?,
    @ColumnInfo(name = "min_affection") val minAffection: Int?,
    @ColumnInfo(name = "relative_physical_stats") val relativePhysicalStats: Int?,
    @ColumnInfo(name = "party_species_id") val partySpeciesId: Int?,
    @ColumnInfo(name = "party_type") val partyType: Int?,
    @ColumnInfo(name = "trade_species_id") val tradeSpeciesId: Int?,
    val location: String?,
    val region: String?,
    /** 1 female, 2 male. */
    val gender: Int?,
    @ColumnInfo(name = "needs_rain") val needsRain: Boolean,
    @ColumnInfo(name = "turn_upside_down") val turnUpsideDown: Boolean,
    @ColumnInfo(name = "used_move") val usedMove: String?,
    @ColumnInfo(name = "min_move_count") val minMoveCount: Int?,
    @ColumnInfo(name = "min_steps") val minSteps: Int?,
    @ColumnInfo(name = "min_damage_taken") val minDamageTaken: Int?,
)

@Entity(tableName = "type_efficacy", primaryKeys = ["attacking_type", "defending_type"])
data class TypeEfficacyEntity(
    @ColumnInfo(name = "attacking_type") val attackingType: Int,
    @ColumnInfo(name = "defending_type") val defendingType: Int,
    /** Percent: 0, 50, 100 or 200. */
    val factor: Int,
)

/** Game versions, used to label downloaded flavor text. */
@Entity(tableName = "game_version")
data class GameVersionEntity(
    @PrimaryKey val id: Int,
    val name: String,
)

/**
 * The search index: one row per species and language. `search` holds normalized tokens plus a compact form, for
 * example `mr mime mrmime`, so queries without punctuation still match.
 */
@Fts5(
    tokenizer = FtsOptions.TOKENIZER_UNICODE61,
    tokenizerArgs = ["remove_diacritics 2"],
    prefix = [2, 3],
    notIndexed = ["species_id", "language_id"],
)
@Entity(tableName = "species_name_fts")
internal data class SpeciesNameFtsEntity(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowId: Int,
    @ColumnInfo(name = "species_id") val speciesId: Int,
    @ColumnInfo(name = "language_id") val languageId: Int,
    val search: String,
)
