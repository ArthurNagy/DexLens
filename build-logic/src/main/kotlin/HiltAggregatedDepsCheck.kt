import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Fails when a module's Hilt code generation came out incomplete (ADR 0015): every main Kotlin class annotated
 * with a Hilt entry annotation must have its `hilt_aggregated_deps` file in the variant's KSP Java output.
 * KSP 2.3.13 could finish "successfully" with that output partly or wholly missing (google/ksp#3286), and the
 * build went on to ship an app without the dropped bindings or screens.
 *
 * Hilt names each file `_<package with dots as underscores>_<rest>.java`, where `<rest>` is, per annotation:
 * - `@InstallIn` module: `<Class>`, or `HiltWrapper_<Class>` when the module isn't public (Kotlin `internal`)
 * - `@HiltViewModel`: `<Class>_HiltModules_BindsModule` and `<Class>_HiltModules_KeyModule`
 * - `@AndroidEntryPoint` / `@HiltAndroidApp`: `<Class>_GeneratedInjector`
 * - `@HiltWorker` (androidx.hilt): `<Class>_HiltModule`
 * so a class counts as covered when some file's `<rest>`, without a leading `HiltWrapper_`, is `<Class>` or
 * starts with `<Class>_` (a nested class shows up inside `<rest>` as `<Outer>_<Class>`).
 *
 * Annotations are found by scanning source text with comments and string literals stripped: a spelling the scan misses (an import
 * alias) only goes unchecked, it never fails the build. No outputs, so it runs on every build that compiles the
 * variant — also when the KSP task is UP-TO-DATE or FROM-CACHE.
 */
abstract class HiltAggregatedDepsCheck : DefaultTask() {
    @get:Input
    abstract val modulePath: Property<String>

    @get:Input
    abstract val variantName: Property<String>

    /** The module's main Kotlin sources. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    /** The variant's KSP Java output directory (`build/generated/ksp/<variant>/java`). */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val kspJavaOutput: ConfigurableFileCollection

    @TaskAction
    fun check() {
        val generated = kspJavaOutput.asFileTree
            .matching { include("$AGGREGATED_DEPS_DIR/*.java") }
            .map { it.nameWithoutExtension }
        val annotated = sources.asFileTree.matching { include("**/*.kt") }.flatMap(::hiltClasses)
        val missing = annotated.filterNot { it.isCoveredBy(generated) }
        val module = modulePath.get()
        val variant = variantName.get()
        logger.info(
            "Hilt aggregated deps in $module ($variant): ${annotated.size - missing.size} of ${annotated.size} Hilt classes covered.",
        )
        if (missing.isEmpty()) return

        val variantTask = variant.replaceFirstChar { it.uppercase() }
        throw GradleException(
            buildString {
                appendLine(
                    "Hilt code generation is incomplete in $module ($variant): KSP wrote no $AGGREGATED_DEPS_DIR " +
                        "entry for ${missing.size} Hilt class(es):",
                )
                missing.forEach { appendLine("  - ${it.qualifiedName} (@${it.annotation}) in ${it.file.name}") }
                appendLine("Their bindings would be missing from the Hilt graph (or R8 would strip their screens).")
                append(
                    "Regenerate with `./gradlew $module:ksp${variantTask}Kotlin --rerun` and check the KSP version " +
                        "(docs/DECISIONS/0015-reliable-hilt-code-generation.md). Searched ${kspJavaOutput.files.joinToString()}.",
                )
            },
        )
    }

    private data class HiltClass(
        val packageName: String,
        val simpleName: String,
        val annotation: String,
        val file: File,
    ) {
        val qualifiedName get() = if (packageName.isEmpty()) simpleName else "$packageName.$simpleName"

        fun isCoveredBy(generated: List<String>): Boolean {
            val prefix = "_" + packageName.replace('.', '_') + "_"
            return generated.any { name ->
                if (!name.startsWith(prefix)) return@any false
                val rest = name.removePrefix(prefix).removePrefix(WRAPPER_PREFIX)
                rest == simpleName || rest.startsWith("${simpleName}_") || rest.contains("_${simpleName}_") || rest.endsWith("_$simpleName")
            }
        }
    }

    private fun hiltClasses(file: File): List<HiltClass> {
        val text = file.readText().replace(STRINGS_AND_COMMENTS, " ")
        val packageName = PACKAGE.find(text)?.groupValues?.get(1)?.replace("`", "").orEmpty()
        return ANNOTATION.findAll(text).mapNotNull { annotation ->
            val declaration = DECLARATION.find(text, annotation.range.last + 1) ?: return@mapNotNull null
            HiltClass(packageName, declaration.groupValues[1], annotation.groupValues[1], file)
        }.distinctBy { it.simpleName }.toList()
    }

    private companion object {
        const val AGGREGATED_DEPS_DIR = "hilt_aggregated_deps"
        const val WRAPPER_PREFIX = "HiltWrapper_"

        /** String literals and comments, matched left to right so a comment opener inside a string (or a quote inside a comment) can't misfire. */
        val STRINGS_AND_COMMENTS = Regex(""""{3}[\s\S]*?"{3}|"(?:\\.|[^"\\\n])*"|/\*[\s\S]*?\*/|//[^\n]*""")
        val PACKAGE = Regex("""^\s*package\s+([\w.`]+)""", RegexOption.MULTILINE)
        val ANNOTATION = Regex("""@(?:[\w.]+\.)?(InstallIn|HiltViewModel|AndroidEntryPoint|HiltAndroidApp|HiltWorker)\b""")

        /** The next class-like declaration; `(?<!::)` skips the `::class` inside the annotation's arguments. */
        val DECLARATION = Regex("""(?<!::)\b(?:class|object|interface)\s+(\w+)""")
    }
}
