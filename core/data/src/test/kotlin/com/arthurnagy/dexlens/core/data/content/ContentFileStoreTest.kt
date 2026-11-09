package com.arthurnagy.dexlens.core.data.content

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
internal class ContentFileStoreTest {
    private val store = ContentFileStore(ApplicationProvider.getApplicationContext<Context>())

    @After
    fun clean() = store.clear()

    // Scenario: Partial files are never used
    @Test
    fun aPartialFileDoesNotCountAsDownloaded() {
        val target = store.file(ArtKind.SPRITE, 25)
        target.parentFile?.mkdirs()
        File(target.parentFile, "${target.name}.partial").writeBytes(byteArrayOf(1))

        assertFalse(store.exists(ArtKind.SPRITE, 25))
        assertEquals(0, store.completedFileCount())
    }

    @Test
    fun writesToTheFinalNameAndCounts() {
        store.write(ArtKind.ARTWORK, 25, byteArrayOf(1, 2))

        assertTrue(store.exists(ArtKind.ARTWORK, 25))
        assertEquals(1, store.completedFileCount())
        assertFalse(store.file(ArtKind.ARTWORK, 25).parentFile!!.list()!!.any { it.endsWith(".partial") })
    }

    // Scenario: Delete frees content
    @Test
    fun clearRemovesEverything() {
        store.write(ArtKind.SPRITE, 1, byteArrayOf(1))
        store.markFlavorImported()

        store.clear()

        assertFalse(store.exists(ArtKind.SPRITE, 1))
        assertFalse(store.isFlavorImported())
    }
}
