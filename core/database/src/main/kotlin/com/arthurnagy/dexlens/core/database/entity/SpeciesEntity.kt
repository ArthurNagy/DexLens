package com.arthurnagy.dexlens.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
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
