package com.arthurnagy.dexlens.core.data.content

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arthurnagy.dexlens.core.testing.TestDispatcherProvider
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
internal class ContentFileStoreTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun TestScope.store() = ContentFileStore(context, TestDispatcherProvider(testScheduler))

    @After
    fun clean() {
        File(context.filesDir, "content").deleteRecursively()
    }

    // Scenario: Partial files are never used
    @Test
    fun aPartialFileDoesNotCountAsDownloaded() = runTest {
        val store = store()
        val target = store.file(25)
        target.parentFile?.mkdirs()
        File(target.parentFile, "${target.name}.partial").writeBytes(byteArrayOf(1))

        assertEquals(emptySet(), store.storedIds())
    }

    @Test
    fun writesToTheFinalNameAndCounts() = runTest {
        val store = store()
        store.write(25, byteArrayOf(1, 2))

        assertEquals(setOf(25), store.storedIds())
        assertFalse(store.file(25).parentFile!!.list()!!.any { it.endsWith(".partial") })
    }

    @Test
    fun nothingIsStoredBeforeTheFirstWrite() = runTest {
        assertEquals(emptySet(), store().storedIds())
    }

    // Scenario: Delete frees content
    @Test
    fun clearRemovesEverything() = runTest {
        val store = store()
        store.write(1, byteArrayOf(1))
        store.markFlavorImported()
        assertTrue(store.isFlavorImported())

        store.clear()

        assertEquals(emptySet(), store.storedIds())
        assertFalse(store.isFlavorImported())
    }
}
