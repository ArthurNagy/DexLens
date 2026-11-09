plugins {
    id("android-forge.android.library")
    id("android-forge.hilt")
}

android {
    namespace = "com.arthurnagy.dexlens.core.navigation"
}

dependencies {
    api(libs.androidx.navigation3.runtime)
}
