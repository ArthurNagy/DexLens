plugins {
    id("android-forge.feature.impl")
}

android {
    namespace = "com.arthurnagy.dexlens.feature.settings.impl"
    resourcePrefix = "settings_"
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation3.ui)
    api(project(":feature:settings:api"))
}
