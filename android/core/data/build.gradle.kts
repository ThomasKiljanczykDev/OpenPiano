plugins {
    alias(libs.plugins.openpiano.android.library)
    alias(libs.plugins.openpiano.android.hilt)
}

android {
    namespace = "dev.thomas_kiljanczyk.openpiano.core.data"
    // android.util.Log is called on the IOException paths exercised by unit tests.
    testOptions.unitTests.isReturnDefaultValues = true
}

dependencies {
    api(projects.core.model)
    api(projects.core.datastoreProto)
    implementation(projects.core.common)
    implementation(libs.androidx.datastore)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
