import com.android.build.api.dsl.LibraryExtension
import dev.thomas_kiljanczyk.openpiano.buildlogic.configureKotlinAndroid
import dev.thomas_kiljanczyk.openpiano.buildlogic.int
import dev.thomas_kiljanczyk.openpiano.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("openpiano.kotlin.quality")

        extensions.configure<LibraryExtension> {
            configureKotlinAndroid(this)
            if (file("consumer-rules.pro").exists()) {
                defaultConfig.consumerProguardFiles("consumer-rules.pro")
            }
            defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            testOptions.targetSdk = libs.int("targetSdk")
        }
    }
}
