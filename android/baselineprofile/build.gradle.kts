plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.openpiano.kotlin.quality)
}

android {
    namespace = "dev.thomas_kiljanczyk.openpiano.baselineprofile"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = 29
        targetSdk = libs.versions.targetSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Profile merges into main, so one flavor covers both.
        missingDimensionStrategy("distribution", "foss")
    }

    targetProjectPath = ":app"

    testOptions {
        managedDevices {
            localDevices {
                create("pixel6Api36") {
                    device = "Pixel 6"
                    apiLevel = 36
                    systemImageSource = "google-atd"
                    require64Bit = true
                }
            }
        }
    }
}

baselineProfile {
    managedDevices += "pixel6Api36"
    useConnectedDevices = false
}

dependencies {
    implementation(projects.feature.keyboard.impl)

    implementation(libs.androidx.test.junit)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}

androidComponents {
    onVariants { variant ->
        val artifactsLoader = variant.artifacts.getBuiltArtifactsLoader()
        variant.instrumentationRunnerArguments.put(
            "targetAppId",
            variant.testedApks.map { artifactsLoader.load(it)?.applicationId },
        )
    }
}
