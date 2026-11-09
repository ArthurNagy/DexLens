package com.arthurnagy.dexlens.core.database.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Fts5
import androidx.room3.FtsOptions
import androidx.room3.PrimaryKey

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
