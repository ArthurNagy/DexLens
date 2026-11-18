import com.android.build.api.variant.AndroidComponentsExtension
import com.google.devtools.ksp.gradle.KspAATask

/*
 * Hilt (Dagger) on KSP for an Android module: applied after an android-forge Android convention.
 * Constructor injection everywhere; @AndroidEntryPoint only on framework entry points
 * (Activity, Service, BroadcastReceiver). See ADR 0002.
 *
 * Per variant, `check<Variant>HiltAggregatedDeps` fails the build when KSP left out a Hilt class's
 * hilt_aggregated_deps entry (HiltAggregatedDepsCheck, ADR 0015). The variant's Java compile depends
 * on it, so every assemble and the CI gate run it.
 */
plugins {
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    "implementation"(catalog.findLibrary("hilt-android").get())
    "ksp"(catalog.findLibrary("hilt-compiler").get())
}

val projectPath = path

// Application and library modules register different public types under the same name.
(extensions.getByName("androidComponents") as AndroidComponentsExtension<*, *, *>).onVariants { variant ->
    val variantTask = variant.name.replaceFirstChar { it.uppercase() }
    // KSP registers its per-variant task after this callback runs, so look it up lazily: by type first, so
    // resolving the outputs realizes only KSP tasks.
    val kspTask = "ksp${variantTask}Kotlin"
    val check = tasks.register<HiltAggregatedDepsCheck>("check${variantTask}HiltAggregatedDeps") {
        group = "verification"
        description = "Fails when KSP left out a Hilt class's hilt_aggregated_deps entry in the ${variant.name} variant."
        modulePath = projectPath
        variantName = variant.name
        listOfNotNull(variant.sources.kotlin, variant.sources.java).forEach { sources.from(it.static) }
        kspOutputs.from(tasks.withType<KspAATask>().named { it == kspTask })
        dependsOn(kspTask)
    }
    tasks.named { it == "compile${variantTask}JavaWithJavac" }.configureEach { dependsOn(check) }
}
