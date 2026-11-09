plugins {
    id("android-forge.android.library.compose")
}

android {
    namespace = "com.arthurnagy.dexlens.core.designsystem"
}

dependencies {
    api(project(":core:model"))
}
