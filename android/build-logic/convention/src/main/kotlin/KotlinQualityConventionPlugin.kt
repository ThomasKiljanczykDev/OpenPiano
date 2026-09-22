import dev.thomas_kiljanczyk.openpiano.buildlogic.configureDetekt
import org.gradle.api.Plugin
import org.gradle.api.Project

class KotlinQualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = target.configureDetekt()
}
