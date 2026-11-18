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
 * Fails when a module's Hilt code generation came out incomplete (ADR 0015): every Kotlin class in the variant's
 * source directories annotated with a Hilt entry annotation must have its `hilt_aggregated_deps` file in the
 * variant's KSP output. KSP 2.3.13 could finish "successfully" with that output partly or wholly missing
 * (google/ksp#3286), and the build went on to ship an app without the dropped bindings or screens.
 *
 * Hilt names each file `_<package with dots as underscores>_<rest>.java`. `<rest>` starts with the class path —
 * the enclosing classes and the class joined by `_` (`Outer_Inner`) — optionally after `HiltWrapper_` (Hilt's name
 * for a module that isn't public, such as a Kotlin `internal` one). Per annotation, a class is covered when:
 * - `@HiltViewModel`: both `<path>_HiltModules_BindsModule` and `<path>_HiltModules_KeyModule` exist
 * - `@InstallIn`, `@AndroidEntryPoint`, `@HiltAndroidApp`, `@HiltWorker`: some `<rest>` is `<path>` or `<path>_…`
 *   (today: `<path>`, `<path>_GeneratedInjector`, `<path>_GeneratedInjector`, `<path>_HiltModule`)
 * The package prefix must match exactly, so a same-named class in a subpackage never covers another.
 *
 * Classes are found by scanning source text with string literals and comments stripped. A spelling the scan misses
 * (an import alias) only goes unchecked, and a class whose nesting the scan can't work out (inside a function body,
 * say) is matched on its simple name alone: the guard may miss a loss, but never fails a good build. It has no
 * outputs, so it runs on every build that compiles the variant, also when the KSP task is UP-TO-DATE or FROM-CACHE.
 */
abstract class HiltAggregatedDepsCheck : DefaultTask() {
    @get:Input
    abstract val modulePath: Property<String>

    @get:Input
    abstract val variantName: Property<String>

    /** The variant's static Kotlin and Java source directories (`src/main`, `src/<buildType>`, `src/<variant>`). */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    /** The variant's KSP task outputs; `hilt_aggregated_deps/` sits at the root of its Java output directory. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val kspOutputs: ConfigurableFileCollection

    @TaskAction
    fun check() {
        val generated = kspOutputs.asFileTree
            .matching { include("$AGGREGATED_DEPS_DIR/*.java") }
            .map { it.nameWithoutExtension }
        val annotated = sources.asFileTree.matching { include("**/*.kt") }.flatMap(::hiltClasses)
        val missing = annotated.associateWith { it.missingEntries(generated) }.filterValues { it.isNotEmpty() }
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
                missing.forEach { (hiltClass, entries) ->
                    appendLine(
                        "  - ${hiltClass.qualifiedName} (@${hiltClass.annotation}) in ${hiltClass.file.name}, " +
                            "expected ${entries.joinToString(" and ")}",
                    )
                }
                appendLine("Their bindings would be missing from the Hilt graph (or R8 would strip their screens).")
                append(
                    "Regenerate with `./gradlew $module:ksp${variantTask}Kotlin --rerun` and check the KSP version " +
                        "(docs/DECISIONS/0015-reliable-hilt-code-generation.md). Searched ${kspOutputs.files.joinToString()}.",
                )
            },
        )
    }

    /**
     * A Hilt-annotated class. [outerClasses] are its enclosing classes, outermost first, or null when the scan
     * couldn't work them out.
     */
    private data class HiltClass(
        val packageName: String,
        val outerClasses: List<String>?,
        val simpleName: String,
        val annotation: String,
        val file: File,
    ) {
        private val path = (outerClasses.orEmpty() + simpleName).joinToString("_")

        val qualifiedName get() = (listOf(packageName).filter { it.isNotEmpty() } + outerClasses.orEmpty() + simpleName).joinToString(".")

        /** The expected entries (as `_<pkg>_<rest>` patterns) that no generated file matches. */
        fun missingEntries(generated: List<String>): List<String> {
            val prefix = "_" + packageName.replace('.', '_') + "_"
            val rests = generated.filter { it.startsWith(prefix) }.map { it.removePrefix(prefix).removePrefix(WRAPPER_PREFIX) }
            val pathPattern = if (outerClasses != null) Regex.escape(path) else """(?:\w+_)?${Regex.escape(simpleName)}"""
            val suffixes = REQUIRED_SUFFIXES[annotation]
            val expected = suffixes?.associateWith { Regex(pathPattern + Regex.escape(it)) }
                ?: mapOf("" to Regex("""$pathPattern(?:_\w+)?"""))
            return expected.filter { (_, pattern) -> rests.none(pattern::matches) }
                .keys.map { suffix -> if (suffix.isEmpty()) "$prefix[$WRAPPER_PREFIX]$path*" else "$prefix$path$suffix" }
        }
    }

    private fun hiltClasses(file: File): List<HiltClass> {
        val text = file.readText().replace(STRINGS_AND_COMMENTS, " ")
        val packageName = PACKAGE.find(text)?.groupValues?.get(1)?.replace("`", "").orEmpty()
        val declarations = DECLARATION.findAll(text).map { match ->
            Declaration(match.range.first, match.range.last, match.groupValues[1].ifEmpty { match.groupValues[2] }.ifEmpty { "Companion" })
        }.toList()
        return ANNOTATION.findAll(text).mapNotNull { annotation ->
            val declaration = declarations.firstOrNull { it.start > annotation.range.last } ?: return@mapNotNull null
            HiltClass(packageName, outerClasses(text, declarations, declaration.start), declaration.name, annotation.groupValues[1], file)
        }.distinct().toList()
    }

    private data class Declaration(
        val start: Int,
        val end: Int,
        val name: String,
    )

    /**
     * The classes whose bodies enclose [position]: for each brace still open there, the class declared right before
     * it. Null when an open brace isn't a class body (a function body or initializer) or the scan can't tell.
     */
    private fun outerClasses(
        text: String,
        declarations: List<Declaration>,
        position: Int,
    ): List<String>? {
        val openBraces = ArrayDeque<Int>()
        for (i in 0 until position) {
            when (text[i]) {
                '{' -> openBraces.addLast(i)
                '}' -> openBraces.removeLastOrNull()
            }
        }
        return openBraces.map { brace ->
            val owner = declarations.lastOrNull { it.end < brace } ?: return null
            val between = text.substring(owner.end + 1, brace)
            if ('{' in between || '}' in between || NOT_A_CLASS_BODY.containsMatchIn(between)) return null
            owner.name
        }
    }

    private companion object {
        const val AGGREGATED_DEPS_DIR = "hilt_aggregated_deps"
        const val WRAPPER_PREFIX = "HiltWrapper_"

        /** Annotations with fixed entry names; the others accept `<path>` or any `<path>_…`. */
        val REQUIRED_SUFFIXES = mapOf("HiltViewModel" to listOf("_HiltModules_BindsModule", "_HiltModules_KeyModule"))

        /**
         * String literals and comments, matched left to right so a comment opener inside a string (or a quote inside
         * a comment) can't misfire. The string loop is possessive (`*+`): a backtracking loop overflows the stack on a
         * long literal.
         */
        val STRINGS_AND_COMMENTS = Regex(""""{3}[\s\S]*?"{3}|"(?:\\.|[^"\\\n])*+"|/\*[\s\S]*?\*/|//[^\n]*""")
        val PACKAGE = Regex("""^\s*package\s+([\w.`]+)""", RegexOption.MULTILINE)
        val ANNOTATION = Regex("""@(?:[\w.]+\.)?(InstallIn|HiltViewModel|AndroidEntryPoint|HiltAndroidApp|HiltWorker)\b""")

        /** A class-like declaration; `(?<!::)` skips the `::class` inside an annotation's arguments. */
        val DECLARATION = Regex("""(?<!::)\b(?:class|object|interface)\s+(\w+)|\bcompanion\s+object\b(?:\s+(\w+))?""")
        val NOT_A_CLASS_BODY = Regex("""\b(?:fun|init|get|set)\b|=""")
    }
}
