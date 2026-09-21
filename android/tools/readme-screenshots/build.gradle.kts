plugins {
    alias(libs.plugins.openpiano.android.library)
    alias(libs.plugins.openpiano.android.library.compose)
    alias(libs.plugins.android.compose.screenshot)
}

android {
    namespace = "dev.thomas_kiljanczyk.openpiano.tools.readmescreenshots"
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

tasks.withType<Test>().configureEach {
    maxHeapSize = "6g"
}

fun DependencyHandlerScope.screenshotDependency(dependency: Any) {
    implementation(dependency)
    screenshotTestImplementation(dependency)
}

dependencies {
    screenshotDependency(projects.tools.screenshotMocks)
    screenshotDependency(projects.core.ui)
    screenshotDependency(projects.core.designsystem)
    screenshotDependency(projects.core.model)
    screenshotDependency(projects.core.data)
    screenshotDependency(projects.feature.keyboard.impl)
    screenshotDependency(projects.feature.settings.impl)

    screenshotDependency(libs.androidx.compose.ui)
    screenshotDependency(libs.androidx.compose.ui.tooling.preview)
    screenshotDependency(libs.androidx.compose.material3)

    screenshotTestImplementation(libs.android.screenshot.validation.api)
}
