package com.arthurnagy.dexlens.core.database

import androidx.room3.ColumnInfo

data class SpeciesSummaryRow(
    val id: Int,
    val name: String,
    @ColumnInfo(name = "type_primary") val typePrimary: Int,
    @ColumnInfo(name = "type_secondary") val typeSecondary: Int?,
)

data class SpeciesSearchRow(
    @ColumnInfo(name = "species_id") val speciesId: Int,
    @ColumnInfo(name = "language_id") val languageId: Int,
    val name: String,
)

data class VarietyAbilityRow(
    @ColumnInfo(name = "variety_id") val varietyId: Int,
    val slot: Int,
    @ColumnInfo(name = "is_hidden") val isHidden: Boolean,
    val name: String,
    @ColumnInfo(name = "short_effect") val shortEffect: String?,
    val effect: String?,
)

data class ChainSpeciesRow(
    val id: Int,
    val name: String,
    @ColumnInfo(name = "evolves_from_species_id") val evolvesFromSpeciesId: Int?,
    @ColumnInfo(name = "type_primary") val typePrimary: Int,
    @ColumnInfo(name = "type_secondary") val typeSecondary: Int?,
)

data class ScanNameRow(
    @ColumnInfo(name = "species_id") val speciesId: Int,
    val name: String,
)

/** A generation and the lowest National Pokédex number in it. */
data class GenerationStartRow(
    val generation: Int,
    @ColumnInfo(name = "first_species_id") val firstSpeciesId: Int,
)
