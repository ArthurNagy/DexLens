plugins {
    id("android-forge.android.library")
    id("android-forge.hilt")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.arthurnagy.dexlens.core.network"
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        // The API every remote data source talks to. A per-environment value (a staging server for debug) goes in
        // buildTypes { debug { buildConfigField(...) } } — android-forge projects use build types, never product flavors.
        buildConfigField("String", "API_BASE_URL", "\"https://raw.githubusercontent.com/\"")
    }
}

dependencies {
    api(project(":core:domain")) // DomainError, returned by catchNetwork
    api(libs.kotlinx.serialization.json) // DTOs are @Serializable
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.timber)
    testImplementation(libs.okhttp.mockwebserver3)
}
