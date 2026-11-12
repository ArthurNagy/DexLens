/*
 * Baseline Profile + startup profile generation and Macrobenchmark for :app. Runs on a device or emulator
 * (API 33+, or rooted), never in the PR gate.
 *
 * - `./gradlew :app:generateReleaseBaselineProfile` regenerates
 *   app/src/release/generated/baselineProfiles/{baseline-prof.txt,startup-prof.txt} — commit both.
 * - `./gradlew :benchmarks:connectedBenchmarkReleaseAndroidTest` measures cold startup and scrolling with and
 *   without the profile.
 */
plugins {
    id("com.android.test")
    id("androidx.baselineprofile")
    id("android-forge.spotless")
}

android {
    namespace = "com.arthurnagy.dexlens.benchmarks"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()
    defaultConfig {
        // The app's minSdk, but at least 28: BaselineProfileRule is @RequiresApi(28).
        minSdk = maxOf(28, libs.versions.androidMinSdk.get().toInt())
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    targetProjectPath = ":app"
}

baselineProfile {
    // The emulator or phone that is connected; no Gradle Managed Device to download.
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.uiautomator)
}
