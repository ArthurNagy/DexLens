pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "DexLens"

include(":app")
include(":core:model")
include(":core:domain")
include(":core:data")
include(":core:datastore")
include(":core:designsystem")
include(":core:ui")
include(":core:navigation")
include(":core:testing")
include(":core:network")
include(":core:database")
include(":feature:pokedex:api")
include(":feature:pokedex:impl")
include(":feature:entry:api")
include(":feature:entry:impl")
include(":feature:settings:api")
include(":feature:settings:impl")
