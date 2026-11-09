plugins {
    id("android-forge.android.library")
    id("android-forge.hilt")
}

android {
    namespace = "com.arthurnagy.dexlens.core.data"
    resourcePrefix = "core_data_"
}

// The corpus test opens the real prebuilt asset, which needs FTS5 on the host JVM (docs/DECISIONS/0010-prebuilt-database-fts5.md).
configurations.matching { it.name.endsWith("UnitTestRuntimeClasspath") }.configureEach {
    exclude(group = "androidx.sqlite", module = "sqlite-bundled-android")
}

dependencies {
    api(project(":core:domain"))
    implementation(project(":core:datastore"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    // android-forge:core-data-deps
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.core.ktx)
    ksp(libs.androidx.hilt.compiler)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.okhttp.mockwebserver3)
    testImplementation(libs.retrofit)
    testImplementation(libs.okhttp)
    testRuntimeOnly(libs.androidx.sqlite.bundled.jvm)
    testImplementation(libs.androidx.sqlite.bundled)
    testImplementation(libs.room3.runtime)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    implementation(libs.timber)
}
