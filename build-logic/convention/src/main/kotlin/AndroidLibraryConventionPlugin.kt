import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Módulo Android sin UI. */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply("org.jetbrains.kotlin.android")
            extensions.configure<LibraryExtension> {
                configureAndroid(this)
            }
            dependencies.add("api", libs.lib("kotlinx-coroutines-core"))
            dependencies.add("implementation", libs.lib("androidx-core-ktx"))
            dependencies.add("testImplementation", libs.lib("junit"))
        }
    }
}
