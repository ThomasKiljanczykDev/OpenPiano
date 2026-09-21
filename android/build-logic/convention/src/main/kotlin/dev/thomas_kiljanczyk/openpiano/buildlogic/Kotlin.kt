package dev.thomas_kiljanczyk.openpiano.buildlogic

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.ManagedVirtualDevice
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

const val JVM_TARGET_VERSION = 17

private const val GMD_API_LEVEL = 34

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun VersionCatalog.int(alias: String): Int = findVersion(alias).get().requiredVersion.toInt()

fun Project.configureKotlinAndroid(extension: CommonExtension) {
    extension.apply {
        compileSdk = libs.int("compileSdk")
        defaultConfig.minSdk = libs.int("minSdk")
        compileOptions.sourceCompatibility = JavaVersion.VERSION_17
        compileOptions.targetCompatibility = JavaVersion.VERSION_17
    }

    configureManagedDevices(extension)

    extensions.getByType<KotlinAndroidProjectExtension>().apply {
        jvmToolchain(JVM_TARGET_VERSION)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            allWarningsAsErrors.set(true)
        }
    }
}

fun configureManagedDevices(extension: CommonExtension) {
    extension.testOptions.managedDevices.allDevices.maybeCreate(
        "pixel6Api34",
        ManagedVirtualDevice::class.java,
    ).apply {
        device = "Pixel 6"
        apiLevel = GMD_API_LEVEL
        systemImageSource = "aosp-atd"
        require64Bit = true
        // AGP 10 defaults this to arm64-v8a, which the aosp-atd image cannot run.
        testedAbi = "x86_64"
    }
}
