import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import dev.thomas_kiljanczyk.openpiano.buildlogic.AppVersion
import java.util.Properties

plugins {
    alias(libs.plugins.openpiano.android.application)
    alias(libs.plugins.openpiano.android.application.compose)
    alias(libs.plugins.openpiano.android.hilt)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

// No keystore: no release signingConfig, unsigned release APK.
val keystoreProperties: Properties =
    providers.fileContents(rootProject.layout.projectDirectory.file("keystore.properties"))
        .asText
        .map { text -> Properties().apply { load(text.reader()) } }
        .getOrElse(Properties())

fun signingValue(propertyKey: String, environmentName: String): String? =
    keystoreProperties.getProperty(propertyKey)
        ?: providers.environmentVariable(environmentName).orNull

val releaseStoreFile: String? = signingValue("storeFile", "RELEASE_KEYSTORE_PATH")

android {
    namespace = "dev.thomas_kiljanczyk.openpiano"

    defaultConfig {
        applicationId = "dev.thomas_kiljanczyk.openpiano"
        versionCode = AppVersion.versionCode
        versionName = AppVersion.versionName
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = signingValue("storePassword", "RELEASE_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "RELEASE_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "RELEASE_KEY_PASSWORD")
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = false
            }
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("foss") {
            dimension = "distribution"
            configure<CrashlyticsExtension> {
                mappingFileUploadEnabled = false
                nativeSymbolUploadEnabled = false
            }
        }
        create("play") {
            dimension = "distribution"
            configure<CrashlyticsExtension> {
                nativeSymbolUploadEnabled = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release")
            ndk.debugSymbolLevel = "FULL"
        }
    }
}

baselineProfile {
    mergeIntoMain = true
}

dependencies {
    implementation(projects.core.analytics)
    implementation(projects.core.audio)
    implementation(projects.core.common)
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
    implementation(projects.core.midi)
    implementation(projects.core.ui)
    implementation(projects.feature.keyboard.impl)
    implementation(projects.feature.settings.impl)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.profileinstaller)

    baselineProfile(projects.baselineprofile)

    "playImplementation"(platform(libs.firebase.bom))
    "playImplementation"(libs.firebase.analytics)
    "playImplementation"(libs.firebase.crashlytics)
    "playImplementation"(libs.firebase.crashlytics.ndk)
}
