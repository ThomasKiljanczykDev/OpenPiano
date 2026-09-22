plugins {
    alias(libs.plugins.openpiano.android.library)
    alias(libs.plugins.openpiano.android.library.compose)
}

android {
    namespace = "dev.thomas_kiljanczyk.openpiano.core.designsystem"
}

dependencies {
    api(libs.androidx.compose.material3)
}
