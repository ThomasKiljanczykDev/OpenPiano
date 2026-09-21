plugins {
    alias(libs.plugins.openpiano.android.feature)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.thomas_kiljanczyk.openpiano.feature.settings.impl"
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.midi)

    implementation(libs.androidx.core.ktx)

    testImplementation(projects.core.testing)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(projects.core.testing)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.espresso.core)
}
