package com.arthurnagy.dexlens.core.database

import androidx.room3.Dao
import androidx.room3.Query
import com.arthurnagy.dexlens.core.database.entity.EggGroupEntity
import com.arthurnagy.dexlens.core.database.entity.EvolutionEntity
import com.arthurnagy.dexlens.core.database.entity.FormEntity
import com.arthurnagy.dexlens.core.database.entity.LanguageEntity
import com.arthurnagy.dexlens.core.database.entity.SpeciesEntity
import com.arthurnagy.dexlens.core.database.entity.SpeciesNameEntity
import com.arthurnagy.dexlens.core.database.entity.VarietyEntity

/** Reads species from the prebuilt, read-only database. English (language 9) is the fallback for every name. */
@Dao
interface SpeciesDao {
    @Query(
        """
        SELECT s.id AS id, COALESCE(l.name, en.name) AS name, v.type_primary AS type_primary, v.type_secondary AS type_secondary
        FROM species s
        JOIN variety v ON v.species_id = s.id AND v.is_default = 1
        JOIN species_name en ON en.species_id = s.id AND en.language_id = 9
        LEFT JOIN species_name l ON l.species_id = s.id AND l.language_id = :languageId
        ORDER BY s.id
        """,
    )
    suspend fun summaries(languageId: Int): List<SpeciesSummaryRow>

    @Query(
        """
        SELECT f.species_id AS species_id, f.language_id AS language_id, n.name AS name
        FROM species_name_fts f
        JOIN species_name n ON n.species_id = f.species_id AND n.language_id = f.language_id
        WHERE species_name_fts MATCH :matchQuery
        """,
    )
    suspend fun searchNames(matchQuery: String): List<SpeciesSearchRow>

    @Query("SELECT * FROM species WHERE id = :speciesId")
    suspend fun species(speciesId: Int): SpeciesEntity?

    @Query("SELECT * FROM species_name WHERE species_id = :speciesId")
    suspend fun speciesNames(speciesId: Int): List<SpeciesNameEntity>

    @Query("SELECT * FROM language")
    suspend fun languages(): List<LanguageEntity>

    @Query("SELECT * FROM variety WHERE species_id = :speciesId ORDER BY sort_order, id")
    suspend fun varieties(speciesId: Int): List<VarietyEntity>

    @Query(
        """
        SELECT f.* FROM form f JOIN variety v ON v.id = f.variety_id
        WHERE v.species_id = :speciesId ORDER BY f.sort_order, f.id
        """,
    )
    suspend fun forms(speciesId: Int): List<FormEntity>

    @Query(
        """
        SELECT va.variety_id AS variety_id, va.slot AS slot, va.is_hidden AS is_hidden,
            a.name AS name, a.short_effect AS short_effect, a.effect AS effect
        FROM variety_ability va
        JOIN ability a ON a.id = va.ability_id
        JOIN variety v ON v.id = va.variety_id
        WHERE v.species_id = :speciesId
        ORDER BY va.variety_id, va.slot
        """,
    )
    suspend fun abilities(speciesId: Int): List<VarietyAbilityRow>

    @Query(
        """
        SELECT g.* FROM egg_group g JOIN species_egg_group sg ON sg.egg_group_id = g.id
        WHERE sg.species_id = :speciesId ORDER BY g.name
        """,
    )
    suspend fun eggGroups(speciesId: Int): List<EggGroupEntity>

    @Query(
        """
        SELECT s.id AS id, COALESCE(l.name, en.name) AS name, s.evolves_from_species_id AS evolves_from_species_id,
            v.type_primary AS type_primary, v.type_secondary AS type_secondary
        FROM species s
        JOIN variety v ON v.species_id = s.id AND v.is_default = 1
        JOIN species_name en ON en.species_id = s.id AND en.language_id = 9
        LEFT JOIN species_name l ON l.species_id = s.id AND l.language_id = :languageId
        WHERE s.evolution_chain_id = :chainId
        ORDER BY s.id
        """,
    )
    suspend fun chainSpecies(
        chainId: Int,
        languageId: Int,
    ): List<ChainSpeciesRow>

    @Query(
        """
        SELECT e.* FROM evolution e JOIN species s ON s.id = e.species_id
        WHERE s.evolution_chain_id = :chainId ORDER BY e.id
        """,
    )
    suspend fun evolutions(chainId: Int): List<EvolutionEntity>

    /** Names in every language except the romanised Japanese readings, which no game displays. */
    @Query("SELECT species_id, name FROM species_name WHERE language_id != 2")
    suspend fun allScanNames(): List<ScanNameRow>

    @Query("SELECT COUNT(*) FROM species")
    suspend fun speciesCount(): Int

    @Query("SELECT id FROM species ORDER BY id")
    suspend fun speciesIds(): List<Int>
}
