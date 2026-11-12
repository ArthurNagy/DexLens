plugins {
    id("android-forge.android.application")
    id("android-forge.hilt")
    // Consumes the profiles :benchmarks records (app/src/release/generated/baselineProfiles/).
    id("androidx.baselineprofile")
}

android {
    namespace = "com.arthurnagy.dexlens"
    defaultConfig {
        applicationId = "com.arthurnagy.dexlens"
        versionCode = 1
        versionName = "0.1.0"
    }
    splits {
        abi {
            // Only for the release workflow's APK run (-PabiSplits=true): an arm64-v8a APK, about half the universal APK's
            // size, for the phones nearly everyone has; the universal APK covers the rest. Never together with
            // bundleRelease, which fails while splits are on. Play builds its own splits from the AAB; local builds stay
            // a single APK.
            isEnable = providers.gradleProperty("abiSplits").map(String::toBoolean).getOrElse(false)
            reset()
            include("arm64-v8a")
            isUniversalApk = true
        }
    }
    androidResources {
        // Stored uncompressed so it can be memory-mapped, as AGP already does for .tflite files.
        noCompress += "dexlens_references.bin"
    }
}

baselineProfile {
    // Profiles are recorded on a device on demand (`./gradlew :app:generateReleaseBaselineProfile`) and committed,
    // never during an ordinary release build.
    automaticGenerationDuringBuild = false
    // R8 puts the classes in startup-prof.txt first in the DEX files, so a cold start touches fewer pages.
    dexLayoutOptimization = true
}

dependencies {
    baselineProfile(project(":benchmarks"))
    // Installs the Baseline Profile on sideloaded installs too (Play installs it itself).
    implementation(libs.androidx.profileinstaller)
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:data")) // only the app modules depend on :core:data — they assemble the DI graph
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:navigation"))

    // Features: depending on an impl is all it takes — its entries arrive through Hilt multibinding.
    // /android-forge-add-feature adds each one above this marker line — keep it.
    implementation(project(":feature:pokedex:impl"))
    implementation(project(":feature:entry:impl"))
    implementation(project(":feature:settings:impl"))
    // android-forge:feature-modules

    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.compose.material3) // the phone/tablet Material 3 — :app's own, not the app convention's
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.compose.material3.adaptive.navigation3)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.orbit.viewmodel)
    implementation(libs.orbit.compose)
    implementation(libs.timber)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)

    testImplementation(libs.orbit.test)

    androidTestImplementation(libs.mlkit.text.recognition)
}
