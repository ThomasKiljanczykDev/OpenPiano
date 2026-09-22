pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "OpenPiano"

include(":app")
include(":baselineprofile")
include(":core:analytics")
include(":core:audio")
include(":core:common")
include(":core:data")
include(":core:datastore-proto")
include(":core:designsystem")
include(":core:midi")
include(":core:model")
include(":core:testing")
include(":core:ui")
include(":feature:keyboard:impl")
include(":feature:settings:impl")
include(":tools:readme-screenshots")
include(":tools:gplay-screenshots")
include(":tools:screenshot-mocks")
