package com.arthurnagy.dexlens

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mikepenz.aboutlibraries.Libs
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The open-source notices the build generates (build-logic's android-forge.oss-notices), read from the app's
 * resources and assets as the About screen will read them: every library carries its licences' full texts, and
 * the notices Google embeds in its AARs are packaged once each, keyed by the owning artifact.
 *
 * `unitTests` runs only the debug variant, so this reads the debug list. It covers release because every release
 * dependency is also a debug one (debug only adds Compose tooling). A `releaseImplementation` dependency would break
 * that: check `build/generated/aboutLibraries/release` as well if one is ever added.
 */
@RunWith(AndroidJUnit4::class)
class OpenSourceNoticesTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val librariesJson: String by lazy { context.resources.openRawResource(R.raw.aboutlibraries).use { it.reader().readText() } }

    @Test
    fun everyLibraryHasALicenceWithItsFullText() {
        val libraries = Libs.Builder().withJson(librariesJson).build().libraries

        assertTrue(libraries.isNotEmpty(), "The generated library list is empty")
        assertEquals(emptyList(), libraries.filter { it.licenses.isEmpty() }.map { it.uniqueId }, "Libraries without a licence")
        val withoutText = libraries.flatMap { library ->
            library.licenses.filter { it.licenseContent.isNullOrBlank() }.map { "${library.uniqueId} (${it.name}, hash ${it.hash})" }
        }
        assertEquals(emptyList(), withoutText, "Licences without a text — commit it under app/config/aboutlibraries/licenses/")
    }

    @Test
    fun everyLicenceALibraryNamesIsInTheData() {
        // The parser drops a licence id it can't resolve, so a library could silently lose one of several licences.
        val data = JSONObject(librariesJson)
        val licences = data.getJSONObject("licenses").keys().asSequence().toSet()
        val unresolved = data.getJSONArray("libraries").objects().flatMap { library ->
            val id = library.getString("uniqueId")
            library.getJSONArray("licenses").strings().filterNot(licences::contains).map { "$id ($it)" }
        }

        assertEquals(emptyList(), unresolved, "Licence ids missing from the licences map")
    }

    @Test
    fun embeddedNoticesHoldMlKitPlayServicesAndLiteRt() {
        val index = JSONObject(readAsset("embedded_notices/index.json")).toNoticesIndex()

        // Google's oss-licenses pair: an index of offsets plus the text it points into.
        listOf("com.google.mlkit:text-recognition", "com.google.android.gms:play-services-basement").forEach { module ->
            val notices = index.noticesOf(module)
            assertEquals(setOf("third_party_licenses.json", "third_party_licenses.txt"), notices.keys, "Notices of $module")
            JSONObject(readAsset(notices.getValue("third_party_licenses.json"))) // a valid offsets index
        }
        // LiteRT's notices for its native code.
        assertEquals(setOf("THIRD_PARTY_NOTICE.txt"), index.noticesOf("com.google.ai.edge.litert:litert").keys)

        index.values.flatMap { it.values }.forEach { asset -> assertTrue(readAsset(asset).isNotBlank(), "Embedded notice $asset is blank") }
    }

    @Test
    fun embeddedNoticesArePackagedOnce() {
        val assets = context.assets.list("embedded_notices").orEmpty().filterNot { it == "index.json" }
        val byContent = assets.groupBy { name -> sha256(context.assets.open("embedded_notices/$name").use { it.readBytes() }) }

        assertTrue(assets.isNotEmpty(), "No embedded notices were packaged")
        assertEquals(emptyList(), byContent.values.filter { it.size > 1 }, "Identical embedded notices packaged more than once")
    }

    /** The notices of [module] (`group:artifact`), whichever version the app resolves. */
    private fun Map<String, Map<String, String>>.noticesOf(module: String): Map<String, String> {
        val owners = keys.filter { it.startsWith("$module:") }
        assertEquals(1, owners.size, "Expected one entry for $module in embedded_notices/index.json, found $owners")
        return getValue(owners.single())
    }

    /** `{ "group:artifact:version": { "file in the AAR": "asset path" } }` as maps. */
    private fun JSONObject.toNoticesIndex(): Map<String, Map<String, String>> = keys().asSequence().associateWith { owner ->
        getJSONObject(owner).let { notices -> notices.keys().asSequence().associateWith(notices::getString) }
    }

    private fun JSONArray.objects(): List<JSONObject> = List(length(), ::getJSONObject)

    private fun JSONArray.strings(): List<String> = List(length(), ::getString)

    private fun readAsset(path: String): String = context.assets.open(path).use { it.reader().readText() }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
