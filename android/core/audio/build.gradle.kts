plugins {
    alias(libs.plugins.openpiano.android.library)
    alias(libs.plugins.openpiano.android.native)
    alias(libs.plugins.openpiano.android.hilt)
}

android {
    namespace = "dev.thomas_kiljanczyk.openpiano.core.audio"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)
    implementation(libs.oboe)
    implementation(libs.kotlinx.coroutines.android)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
