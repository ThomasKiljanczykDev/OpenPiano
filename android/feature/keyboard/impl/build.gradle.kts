plugins {
    alias(libs.plugins.openpiano.android.feature)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.thomas_kiljanczyk.openpiano.feature.keyboard.impl"
}

dependencies {
    implementation(projects.core.audio)
    implementation(projects.core.data)
    implementation(projects.core.midi)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(projects.core.testing)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.espresso.core)
}
