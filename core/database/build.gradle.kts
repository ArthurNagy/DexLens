plugins {
    id("android-forge.android.library")
    id("android-forge.hilt")
    id("android-forge.room") // KSP + androidx.room3 + bundled SQLite + DAO-test deps
}

android {
    namespace = "com.arthurnagy.dexlens.core.database"
}

// FTS5 is missing from the framework driver, so local tests run the real asset on the host-JVM build of the
// bundled driver (docs/DECISIONS/0010-prebuilt-database-fts5.md).
configurations.matching { it.name.endsWith("UnitTestRuntimeClasspath") }.configureEach {
    exclude(group = "androidx.sqlite", module = "sqlite-bundled-android")
}

dependencies {
    implementation(libs.javax.inject)
    testRuntimeOnly(libs.androidx.sqlite.bundled.jvm)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.junit)
}
