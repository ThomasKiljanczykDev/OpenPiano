import com.android.build.api.dsl.LibraryExtension
import dev.thomas_kiljanczyk.openpiano.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("openpiano.android.library")
        pluginManager.apply("openpiano.android.library.compose")
        pluginManager.apply("openpiano.android.hilt")

        extensions.configure<LibraryExtension> {
            defaultConfig.consumerProguardFiles("consumer-rules.pro")
        }

        dependencies {
            add("implementation", project(":core:designsystem"))
            add("implementation", project(":core:ui"))
            add("implementation", project(":core:model"))
            add("implementation", libs.findLibrary("androidx-hilt-navigation-compose").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
            add("implementation", libs.findLibrary("androidx-navigation-compose").get())
        }
    }
}
