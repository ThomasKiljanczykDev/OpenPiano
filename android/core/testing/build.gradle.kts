plugins {
    alias(libs.plugins.openpiano.android.library)
}

android {
    namespace = "dev.thomas_kiljanczyk.openpiano.core.testing"
}

dependencies {
    api(projects.core.audio)
    api(projects.core.data)
    api(projects.core.midi)
    api(projects.core.model)
    api(libs.junit)
    api(libs.kotlinx.coroutines.android)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
}
