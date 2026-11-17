import com.mikepenz.aboutlibraries.plugin.StrictMode
import org.gradle.api.artifacts.component.ModuleComponentIdentifier

/*
 * Open-source notices for an app module, generated per variant from that variant's dependencies — never
 * fetched over the network, so `--offline` builds work and the output is reproducible:
 *
 * 1. The AboutLibraries plugin writes `res/raw/aboutlibraries.json` (every Maven library with its licence
 *    names and full texts). `offlineMode` means it never downloads a licence text: every licence a
 *    dependency declares needs its text committed under `<module>/config/aboutlibraries/licenses/`, and
 *    the module lists the licences it accepts in `aboutLibraries.license.allowedLicenses` — any other
 *    licence, or a library with none, fails the build and names the library. Keep the resource through
 *    resource shrinking with `tools:keep="@raw/aboutlibraries"` in `res/raw/keep.xml`.
 * 2. `collect<Variant>EmbeddedNotices` copies the notices Google embeds in its AARs (Play services / ML Kit
 *    `third_party_licenses.*`, LiteRT `THIRD_PARTY_NOTICE.txt`) into the generated assets as
 *    `embedded_notices/`, keyed by the owning Maven coordinates (see EmbeddedNotices).
 */
plugins {
    id("com.android.application")
    id("com.mikepenz.aboutlibraries.plugin.android")
}

aboutLibraries {
    offlineMode = true
    collect {
        configPath = layout.projectDirectory.dir("config/aboutlibraries")
    }
    library {
        requireLicense = true
    }
    license {
        strictMode = StrictMode.FAIL
    }
}

androidComponents {
    onVariants { variant ->
        // External modules only (project modules carry no notices), and only the AARs among them.
        val runtimeAars = variant.runtimeConfiguration.incoming
            .artifactView { componentFilter { it is ModuleComponentIdentifier } }
            .artifacts
            .resolvedArtifacts
            .map { artifacts -> artifacts.filter { it.file.extension == "aar" }.sortedBy { it.id.componentIdentifier.displayName } }
        val projectDirectory = layout.projectDirectory
        val collect = tasks.register<EmbeddedNotices>("collect${variant.name.replaceFirstChar { it.uppercase() }}EmbeddedNotices") {
            description = "Copies the licence notices embedded in the ${variant.name} runtime AARs into its assets."
            coordinates = runtimeAars.map { artifacts -> artifacts.map { it.id.componentIdentifier.displayName } }
            aars = runtimeAars.map { artifacts -> artifacts.map { projectDirectory.file(it.file.absolutePath) } }
        }
        variant.sources.assets?.addGeneratedSourceDirectory(collect, EmbeddedNotices::outputDirectory)
    }
}
