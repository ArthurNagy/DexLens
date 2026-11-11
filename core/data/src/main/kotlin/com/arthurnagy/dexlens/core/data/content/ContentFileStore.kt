package com.arthurnagy.dexlens.core.data.content

import android.content.Context
import com.arthurnagy.dexlens.core.domain.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Downloaded pictures on disk, one per variety. A file only appears at its final name once fully written, so an
 * interrupted write can never be shown as art, and a resumed download only needs to look at which final names exist.
 * Everything that touches the disk runs on the IO dispatcher, so callers can use it from any thread.
 */
@Singleton
internal class ContentFileStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
        private val dispatchers: DispatcherProvider,
    ) {
        val root: File = File(context.filesDir, "content")

        private val artworkDirectory: File get() = File(root, ARTWORK_DIRECTORY)
        private val flavorMarker: File get() = File(root, FLAVOR_MARKER)
        private val completeMarker: File get() = File(root, COMPLETE_MARKER)

        /** Where a variety's picture lives. This only builds the path and does not touch the disk. */
        fun file(id: Int): File = File(artworkDirectory, "$id$ARTWORK_EXTENSION")

        suspend fun write(
            id: Int,
            bytes: ByteArray,
        ) = onDisk {
            val target = file(id)
            target.parentFile?.mkdirs()
            val partial = File(target.parentFile, "${target.name}.partial")
            partial.writeBytes(bytes)
            if (!partial.renameTo(target)) {
                partial.delete()
                error("Could not store ${target.name}")
            }
        }

        /** The varieties whose picture is fully written. */
        suspend fun storedIds(): Set<Int> = onDisk {
            artworkDirectory
                .list { _, name -> name.endsWith(ARTWORK_EXTENSION) }
                .orEmpty()
                .mapNotNull { it.removeSuffix(ARTWORK_EXTENSION).toIntOrNull() }
                .toSet()
        }

        suspend fun isFlavorImported(): Boolean = onDisk { flavorMarker.isFile }

        suspend fun markFlavorImported() = onDisk {
            root.mkdirs()
            flavorMarker.writeText("done")
        }

        /** A finished run, even if a file was missing upstream. */
        suspend fun isComplete(): Boolean = onDisk { completeMarker.isFile }

        suspend fun markComplete() = onDisk {
            root.mkdirs()
            completeMarker.writeText("done")
        }

        suspend fun clear() {
            onDisk { root.deleteRecursively() }
        }

        private suspend fun <T> onDisk(block: () -> T): T = withContext(dispatchers.io) { block() }

        private companion object {
            const val ARTWORK_DIRECTORY = "artwork"
            const val ARTWORK_EXTENSION = ".webp"
            const val FLAVOR_MARKER = "flavor.done"
            const val COMPLETE_MARKER = "complete.done"
        }
    }
