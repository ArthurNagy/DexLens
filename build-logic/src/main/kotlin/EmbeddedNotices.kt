import groovy.json.JsonOutput
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile

/**
 * Copies the licence notices that Google embeds in its AARs into a generated assets directory:
 * Play services and ML Kit ship `third_party_licenses.json` (an index of `start`/`length` offsets) next
 * to `third_party_licenses.txt`, and LiteRT ships `THIRD_PARTY_NOTICE.txt` for its native code. None of
 * them are in the Maven metadata, so the AboutLibraries plugin never sees them.
 *
 * Writes `embedded_notices/index.json` — `{ "group:artifact:version": { "<file in the AAR>": "<asset path>" } }`,
 * sorted — plus one asset per distinct file, named by its SHA-256, so identical copies (Play services
 * modules of one release share the same notices) are packaged once. Empty notices are skipped.
 */
@CacheableTask
abstract class EmbeddedNotices : DefaultTask() {
    /** Maven coordinates of each runtime AAR, in the same order as [aars]. */
    @get:Input
    abstract val coordinates: ListProperty<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val aars: ListProperty<RegularFile>

    /** Root of the generated assets directory (the notices land in its [ASSET_DIR]). */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun collect() {
        val root = outputDirectory.get().asFile
        root.deleteRecursively()
        val noticesDir = File(root, ASSET_DIR).apply { mkdirs() }
        val index = sortedMapOf<String, Map<String, String>>()
        coordinates.get().zip(aars.get()).forEach { (coordinate, aar) ->
            val notices = ZipFile(aar.asFile).use { zip ->
                NOTICE_FILES.mapNotNull { name ->
                    val entry = zip.getEntry(name) ?: return@mapNotNull null
                    name to zip.getInputStream(entry).use { it.readBytes() }
                }
            }.filterNot { (_, bytes) -> bytes.isEmptyNotice() }
            if (notices.isEmpty()) return@forEach
            index[coordinate] = notices.associateTo(sortedMapOf()) { (name, bytes) ->
                val assetName = "${bytes.sha256()}.${name.substringAfterLast('.')}"
                val asset = File(noticesDir, assetName)
                if (!asset.exists()) asset.writeBytes(bytes)
                name to "$ASSET_DIR/$assetName"
            }
        }
        File(noticesDir, INDEX_FILE).writeText(JsonOutput.prettyPrint(JsonOutput.toJson(index)) + "\n")
    }

    /** Some Firebase / data-transport AARs ship an empty `third_party_licenses.txt` with a `{}` index. */
    private fun ByteArray.isEmptyNotice(): Boolean = toString(Charsets.UTF_8).trim().let { it.isEmpty() || it == "{}" }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256").digest(this).joinToString("") { "%02x".format(it) }

    companion object {
        const val ASSET_DIR = "embedded_notices"
        const val INDEX_FILE = "index.json"

        /** Notice files at the root of an AAR — Google's oss-licenses format, and LiteRT's native notices. */
        val NOTICE_FILES = listOf("third_party_licenses.json", "third_party_licenses.txt", "THIRD_PARTY_NOTICE.txt")
    }
}
