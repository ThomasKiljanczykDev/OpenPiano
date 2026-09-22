package dev.thomas_kiljanczyk.openpiano.buildlogic

import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

fun Project.configureDetekt() {
    pluginManager.apply("dev.detekt")

    val configDir = rootProject.layout.projectDirectory.dir("config/detekt")

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig.set(true)
        parallel.set(true)
        autoCorrect.set(false)
        config.setFrom(configDir.file("detekt.yml"))
        source.setFrom(
            "src/main/kotlin",
            "src/main/java",
            "src/test/kotlin",
            "src/test/java",
            "src/androidTest/kotlin",
            "src/androidTest/java",
        )
    }

    tasks.withType<Detekt>().configureEach {
        jvmTarget.set(JVM_TARGET_VERSION.toString())
        reports {
            html.required.set(true)
            checkstyle.required.set(false)
            sarif.required.set(false)
            markdown.required.set(false)
        }
    }

    dependencies {
        add("detektPlugins", libs.findLibrary("detekt-rules-ktlintWrapper").get())
    }
}
