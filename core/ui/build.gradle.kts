plugins {
    id("android-forge.android.library.compose")
}

android {
    namespace = "com.arthurnagy.dexlens.core.ui"
    resourcePrefix = "core_ui_"
}

dependencies {
    api(project(":core:domain"))
    implementation(project(":core:designsystem"))
    implementation(libs.coil.compose)
    implementation(libs.androidx.compose.material3.adaptive.navigation3)

    testImplementation(libs.androidx.navigation3.ui)
}
