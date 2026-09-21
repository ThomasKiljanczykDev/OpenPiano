import com.android.build.api.dsl.LibraryExtension
import dev.thomas_kiljanczyk.openpiano.buildlogic.SUPPORTED_ABIS
import dev.thomas_kiljanczyk.openpiano.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidNativeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        extensions.configure<LibraryExtension> {
            ndkVersion = libs.findVersion("ndkVersion").get().requiredVersion
            buildFeatures.prefab = true
            externalNativeBuild.cmake.path = file("src/main/cpp/CMakeLists.txt")
            defaultConfig {
                externalNativeBuild.cmake.arguments(
                    "-DANDROID_STL=c++_shared",
                    "-DCMAKE_BUILD_TYPE=Release",
                )
                ndk.abiFilters.addAll(SUPPORTED_ABIS)
            }
        }
    }
}
