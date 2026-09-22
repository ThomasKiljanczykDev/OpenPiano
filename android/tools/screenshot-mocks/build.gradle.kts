plugins {
    alias(libs.plugins.openpiano.android.library)
    alias(libs.plugins.openpiano.android.library.compose)
}

android {
    namespace = "dev.thomas_kiljanczyk.openpiano.tools.screenshotmocks"
}

dependencies {
    api(projects.core.designsystem)
    api(projects.core.model)
    implementation(projects.feature.keyboard.impl)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
}
