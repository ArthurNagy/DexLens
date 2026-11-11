package com.arthurnagy.dexlens.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

/** A game pair, such as Scarlet / Violet, that the entry's game picker can offer. */
@Entity(tableName = "version_group")
data class VersionGroupEntity(
    @PrimaryKey val id: Int,
    val generation: Int,
    /** Release order: higher is newer. */
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
    /** Whether PokeAPI has wild encounters for any of the group's versions. */
    @ColumnInfo(name = "has_encounters") val hasEncounters: Boolean,
)

@Entity(tableName = "version_name", primaryKeys = ["version_id", "language_id"])
data class VersionNameEntity(
    @ColumnInfo(name = "version_id") val versionId: Int,
    @ColumnInfo(name = "language_id") val languageId: Int,
    val name: String,
)

/** A move's current type and power. [power] is null for moves that deal no direct damage. */
@Entity(tableName = "move")
data class MoveEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "type_id") val typeId: Int,
    val power: Int?,
)

@Entity(tableName = "move_name", primaryKeys = ["move_id", "language_id"])
data class MoveNameEntity(
    @ColumnInfo(name = "move_id") val moveId: Int,
    @ColumnInfo(name = "language_id") val languageId: Int,
    val name: String,
)

/** A move's type and power in an older game, written only where they differ from [MoveEntity]'s. */
@Entity(tableName = "move_past_value", primaryKeys = ["move_id", "version_group_id"])
data class MovePastValueEntity(
    @ColumnInfo(name = "move_id") val moveId: Int,
    @ColumnInfo(name = "version_group_id") val versionGroupId: Int,
    @ColumnInfo(name = "type_id") val typeId: Int,
    val power: Int?,
)

/**
 * One level-up move of a variety in a game. [level] 0 means the move is learnt on evolving. The build script writes
 * this table `WITHOUT ROWID`, which Room's validation ignores, so the primary key holds the rows.
 */
@Entity(tableName = "learnset", primaryKeys = ["variety_id", "version_group_id", "move_id", "level"])
data class LearnsetEntity(
    @ColumnInfo(name = "variety_id") val varietyId: Int,
    @ColumnInfo(name = "version_group_id") val versionGroupId: Int,
    @ColumnInfo(name = "move_id") val moveId: Int,
    val level: Int,
)

/** Where a variety is found in the wild in one version, by one method. Written `WITHOUT ROWID` like [LearnsetEntity]. */
@Entity(tableName = "encounter", primaryKeys = ["variety_id", "version_id", "location_id", "method_id"])
data class EncounterEntity(
    @ColumnInfo(name = "variety_id") val varietyId: Int,
    @ColumnInfo(name = "version_id") val versionId: Int,
    @ColumnInfo(name = "location_id") val locationId: Int,
    @ColumnInfo(name = "method_id") val methodId: Int,
    @ColumnInfo(name = "min_level") val minLevel: Int,
    @ColumnInfo(name = "max_level") val maxLevel: Int,
)

@Entity(tableName = "location_name", primaryKeys = ["location_id", "language_id"])
data class LocationNameEntity(
    @ColumnInfo(name = "location_id") val locationId: Int,
    @ColumnInfo(name = "language_id") val languageId: Int,
    val name: String,
)

/**
 * How a Pokémon is found. The app words [identifier] as a string resource. [name] is PokeAPI's English description, the
 * fallback for an identifier it has no string for. Methods that differ only in rate share an identifier.
 */
@Entity(tableName = "encounter_method")
data class EncounterMethodEntity(
    @PrimaryKey val id: Int,
    val identifier: String,
    val name: String,
    @ColumnInfo(name = "sort_order") val sortOrder: Int,
)
