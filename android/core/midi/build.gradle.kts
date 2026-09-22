plugins {
    alias(libs.plugins.openpiano.android.library)
    alias(libs.plugins.openpiano.android.hilt)
}

android {
    namespace = "dev.thomas_kiljanczyk.openpiano.core.midi"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
