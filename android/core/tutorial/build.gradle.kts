plugins {
    alias(libs.plugins.openpiano.android.library)
    alias(libs.plugins.openpiano.android.library.compose)
}

android {
    namespace = "dev.thomas_kiljanczyk.openpiano.core.tutorial"
}

dependencies {
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.junit)
}
