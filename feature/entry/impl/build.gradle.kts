plugins {
    id("android-forge.feature.impl")
}

android {
    namespace = "com.arthurnagy.dexlens.feature.entry.impl"
    resourcePrefix = "entry_"
}

dependencies {
    api(project(":feature:entry:api"))
    implementation(libs.androidx.navigation3.ui)
}
