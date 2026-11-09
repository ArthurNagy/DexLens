package com.arthurnagy.dexlens.core.data.content

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Which picture of a species a file holds. [directory] is its folder inside the content root. */
internal enum class ArtKind(
    val directory: String,
) {
    SPRITE("sprites"),
    ARTWORK("artwork"),
}

/**
 * Downloaded pictures on disk. A file only appears at its final name once fully written, so an interrupted write can
 * never be shown as art, and a resumed download only needs to look at which final names exist.
 */
@Singleton
internal class ContentFileStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        val root: File = File(context.filesDir, "content")

        private val flavorMarker: File get() = File(root, FLAVOR_MARKER)
        private val completeMarker: File get() = File(root, COMPLETE_MARKER)

        fun file(
            kind: ArtKind,
            id: Int,
        ): File = File(File(root, kind.directory), "$id.webp")

        fun exists(
            kind: ArtKind,
            id: Int,
        ): Boolean = file(kind, id).isFile

        fun write(
            kind: ArtKind,
            id: Int,
            bytes: ByteArray,
        ) {
            val target = file(kind, id)
            target.parentFile?.mkdirs()
            val partial = File(target.parentFile, "${target.name}.partial")
            partial.writeBytes(bytes)
            if (!partial.renameTo(target)) {
                partial.delete()
                error("Could not store ${target.name}")
            }
        }

        fun completedFileCount(): Int = ArtKind.entries.sumOf { kind ->
            File(root, kind.directory).list { _, name -> name.endsWith(".webp") }?.size ?: 0
        }

        fun isFlavorImported(): Boolean = flavorMarker.isFile

        fun markFlavorImported() {
            root.mkdirs()
            flavorMarker.writeText("done")
        }

        /** A finished run, even if a file was missing upstream. */
        fun isComplete(): Boolean = completeMarker.isFile

        fun markComplete() {
            root.mkdirs()
            completeMarker.writeText("done")
        }

        fun clear() {
            root.deleteRecursively()
        }

        private companion object {
            const val FLAVOR_MARKER = "flavor.done"
            const val COMPLETE_MARKER = "complete.done"
        }
    }
