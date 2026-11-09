plugins {
    id("android-forge.feature.impl")
}

android {
    namespace = "com.arthurnagy.dexlens.feature.pokedex.impl"
    resourcePrefix = "pokedex_"
}

dependencies {
    api(project(":feature:pokedex:api"))
    implementation(project(":feature:entry:api"))
    implementation(project(":feature:scanner:api"))
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigationevent.compose)
}
