plugins {
    alias(libs.plugins.openpiano.android.library)
    alias(libs.plugins.openpiano.android.hilt)
}

android {
    namespace = "dev.thomas_kiljanczyk.openpiano.core.analytics"
}

dependencies {
    testImplementation(libs.junit)
}
