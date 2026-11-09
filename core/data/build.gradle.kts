plugins {
    id("android-forge.android.library")
    id("android-forge.hilt")
}

android {
    namespace = "com.arthurnagy.dexlens.core.data"
}

dependencies {
    api(project(":core:domain"))
    implementation(project(":core:datastore"))
    implementation(project(":core:network"))
    // android-forge:core-data-deps
    implementation(libs.timber)
}
