import com.android.build.api.dsl.ApplicationExtension
import dev.thomas_kiljanczyk.openpiano.buildlogic.SUPPORTED_ABIS
import dev.thomas_kiljanczyk.openpiano.buildlogic.configureKotlinAndroid
import dev.thomas_kiljanczyk.openpiano.buildlogic.int
import dev.thomas_kiljanczyk.openpiano.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("openpiano.kotlin.quality")

        extensions.configure<ApplicationExtension> {
            configureKotlinAndroid(this)
            defaultConfig {
                targetSdk = libs.int("targetSdk")
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                ndk.abiFilters.addAll(SUPPORTED_ABIS)
            }
        }
    }
}
