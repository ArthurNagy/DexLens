package com.arthurnagy.dexlens.core.database

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Keeps the proof that the prebuilt asset, FTS5 and the bundled driver work together on a real device (ADR 0010). */
@RunWith(AndroidJUnit4::class)
internal class PrebuiltDatabaseDeviceTest {
    // Scenario: Prebuilt file passes validation
    @Test
    fun prebuiltAssetAndFts5OpenOnDevice(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(NAME)
        val database = Room
            .databaseBuilder<DexDatabase>(context, NAME)
            .createFromAsset("databases/dexlens.db")
            .setDriver(BundledSQLiteDriver())
            .build()
        val dao = database.speciesDao()
        assertEquals(1025, dao.speciesCount())
        assertEquals(setOf(669), dao.searchNames("flabebe*").map { it.speciesId }.toSet())
        assertTrue(dao.searchNames("\"mr\"* \"mime\"*").any { it.speciesId == 122 })
        database.close()
        context.deleteDatabase(NAME)
    }

    private companion object {
        const val NAME = "dexlens-device-test.db"
    }
}
