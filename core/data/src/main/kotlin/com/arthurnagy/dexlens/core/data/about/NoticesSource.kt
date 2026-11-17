package com.arthurnagy.dexlens.core.data.about

import android.annotation.SuppressLint
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** The open-source notices :app's build generates (#22). Both reads may throw; the repository catches. */
internal interface NoticesSource {
    /** AboutLibraries' library list, `res/raw/aboutlibraries.json`. */
    fun librariesJson(): String

    /** The text of an asset, such as `embedded_notices/index.json`. */
    fun asset(path: String): String
}

internal class AndroidNoticesSource
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : NoticesSource {
        override fun librariesJson(): String {
            // :core:data can't see :app's R, so the generated resource is found by name. :app's res/raw/keep.xml keeps it
            // (and its name) through resource shrinking.
            @SuppressLint("DiscouragedApi")
            val id = context.resources.getIdentifier(LIBRARIES_RESOURCE, "raw", context.packageName)
            check(id != 0) { "No raw/$LIBRARIES_RESOURCE resource in ${context.packageName}" }
            return context.resources.openRawResource(id).use { it.reader().readText() }
        }

        override fun asset(path: String): String = context.assets.open(path).use { it.reader().readText() }

        private companion object {
            const val LIBRARIES_RESOURCE = "aboutlibraries"
        }
    }
