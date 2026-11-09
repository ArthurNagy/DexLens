package com.arthurnagy.dexlens.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

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
