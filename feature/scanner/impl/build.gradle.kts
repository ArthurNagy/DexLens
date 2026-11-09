plugins {
    id("android-forge.feature.impl")
}

android {
    namespace = "com.arthurnagy.dexlens.feature.scanner.impl"
    resourcePrefix = "scanner_"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":feature:entry:api"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.compose)
    implementation(libs.mlkit.text.recognition)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.junit)
    api(project(":feature:scanner:api"))
}
