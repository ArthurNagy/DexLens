package com.arthurnagy.dexlens.core.database

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Query
import com.arthurnagy.dexlens.core.database.entity.VersionGroupEntity

/** Reads learnsets and wild encounters from the prebuilt database. English (language 9) is the fallback for every name. */
@Dao
interface GameDao {
    /**
     * Whether the variety's moves and locations are its own. Battle-only forms, such as Megas and Gigantamax forms, and
     * varieties with no rows use their species' default variety instead.
     */
    @Query(
        """
        SELECT NOT EXISTS (SELECT 1 FROM form WHERE variety_id = :varietyId AND is_default = 1 AND is_battle_only = 1)
            AND (
                EXISTS (SELECT 1 FROM learnset WHERE variety_id = :varietyId)
                OR EXISTS (SELECT 1 FROM encounter WHERE variety_id = :varietyId)
            )
        """,
    )
    suspend fun usesOwnData(varietyId: Int): Boolean

    @Query("SELECT d.id FROM variety v JOIN variety d ON d.species_id = v.species_id AND d.is_default = 1 WHERE v.id = :varietyId")
    suspend fun defaultVariety(varietyId: Int): Int?

    /** The game pairs with data for the variety, newest first. */
    @Query(
        """
        SELECT vg.* FROM version_group vg
        WHERE EXISTS (SELECT 1 FROM learnset l WHERE l.variety_id = :varietyId AND l.version_group_id = vg.id)
            OR EXISTS (
                SELECT 1 FROM encounter e JOIN game_version gv ON gv.id = e.version_id
                WHERE e.variety_id = :varietyId AND gv.version_group_id = vg.id
            )
        ORDER BY vg.sort_order DESC
        """,
    )
    suspend fun versionGroups(varietyId: Int): List<VersionGroupEntity>

    @Query(
        """
        SELECT gv.id AS id, gv.version_group_id AS version_group_id, COALESCE(l.name, en.name, gv.name) AS name
        FROM game_version gv
        LEFT JOIN version_name en ON en.version_id = gv.id AND en.language_id = 9
        LEFT JOIN version_name l ON l.version_id = gv.id AND l.language_id = :languageId
        WHERE gv.version_group_id IN (:versionGroupIds)
        ORDER BY gv.id
        """,
    )
    suspend fun versions(
        versionGroupIds: List<Int>,
        languageId: Int,
    ): List<VersionRow>

    /** The type and power are the ones the move had in that game. */
    @Query(
        """
        SELECT l.level AS level, COALESCE(n.name, en.name) AS name, COALESCE(p.type_id, m.type_id) AS type_id,
            CASE WHEN p.move_id IS NULL THEN m.power ELSE p.power END AS power
        FROM learnset l
        JOIN move m ON m.id = l.move_id
        JOIN move_name en ON en.move_id = m.id AND en.language_id = 9
        LEFT JOIN move_name n ON n.move_id = m.id AND n.language_id = :languageId
        LEFT JOIN move_past_value p ON p.move_id = m.id AND p.version_group_id = l.version_group_id
        WHERE l.variety_id = :varietyId AND l.version_group_id = :versionGroupId
        ORDER BY l.level, name
        """,
    )
    suspend fun levelUpMoves(
        varietyId: Int,
        versionGroupId: Int,
        languageId: Int,
    ): List<LevelUpMoveRow>

    @Query(
        """
        SELECT e.version_id AS version_id, e.location_id AS location_id, COALESCE(n.name, en.name) AS location,
            e.method_id AS method_id, m.identifier AS method_identifier, m.name AS method, m.sort_order AS method_order, e.min_level AS min_level,
            e.max_level AS max_level
        FROM encounter e
        JOIN game_version gv ON gv.id = e.version_id
        JOIN location_name en ON en.location_id = e.location_id AND en.language_id = 9
        LEFT JOIN location_name n ON n.location_id = e.location_id AND n.language_id = :languageId
        JOIN encounter_method m ON m.id = e.method_id
        WHERE e.variety_id = :varietyId AND gv.version_group_id = :versionGroupId
        ORDER BY e.min_level, location, m.sort_order, e.version_id
        """,
    )
    suspend fun encounters(
        varietyId: Int,
        versionGroupId: Int,
        languageId: Int,
    ): List<EncounterRow>
}

data class VersionRow(
    val id: Int,
    @ColumnInfo(name = "version_group_id") val versionGroupId: Int,
    val name: String,
)

data class LevelUpMoveRow(
    val level: Int,
    val name: String,
    @ColumnInfo(name = "type_id") val typeId: Int,
    val power: Int?,
)

data class EncounterRow(
    @ColumnInfo(name = "version_id") val versionId: Int,
    @ColumnInfo(name = "location_id") val locationId: Int,
    val location: String,
    @ColumnInfo(name = "method_id") val methodId: Int,
    @ColumnInfo(name = "method_identifier") val methodIdentifier: String,
    val method: String,
    @ColumnInfo(name = "method_order") val methodOrder: Int,
    @ColumnInfo(name = "min_level") val minLevel: Int,
    @ColumnInfo(name = "max_level") val maxLevel: Int,
)
