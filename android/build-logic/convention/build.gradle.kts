plugins {
    `kotlin-dsl`
}

group = "dev.thomas_kiljanczyk.openpiano.buildlogic"

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.kotlin.composeCompilerGradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "openpiano.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidApplicationCompose") {
            id = "openpiano.android.application.compose"
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
        register("androidLibrary") {
            id = "openpiano.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "openpiano.android.library.compose"
            implementationClass = "ComposeLibraryConventionPlugin"
        }
        register("androidFeature") {
            id = "openpiano.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidHilt") {
            id = "openpiano.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidNative") {
            id = "openpiano.android.native"
            implementationClass = "AndroidNativeConventionPlugin"
        }
        register("kotlinQuality") {
            id = "openpiano.kotlin.quality"
            implementationClass = "KotlinQualityConventionPlugin"
        }
    }
}
