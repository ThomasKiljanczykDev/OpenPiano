plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.openpiano.kotlin.quality)
}

kotlin {
    jvmToolchain(17)
    compilerOptions.allWarningsAsErrors.set(true)
}

dependencies {
    testImplementation(libs.kotlin.test)
}
