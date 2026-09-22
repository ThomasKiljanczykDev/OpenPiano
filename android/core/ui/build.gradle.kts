plugins {
    alias(libs.plugins.openpiano.android.library)
    alias(libs.plugins.openpiano.android.library.compose)
}

android {
    namespace = "dev.thomas_kiljanczyk.openpiano.core.ui"
}

dependencies {
    api(projects.core.designsystem)
    api(projects.core.model)

    implementation(projects.core.common)
    implementation(libs.androidx.activity.compose)
}
