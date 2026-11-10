plugins {
    id("android-forge.feature.impl")
}

android {
    namespace = "com.arthurnagy.dexlens.feature.pokedex.impl"
    resourcePrefix = "pokedex_"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(project(":feature:pokedex:api"))
    implementation(project(":feature:entry:api"))
    implementation(project(":feature:settings:api"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigationevent.compose)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.compose)
    implementation(libs.mlkit.text.recognition)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.junit)
}
