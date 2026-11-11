import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

/** Módulo :app — el único que conoce las implementaciones. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.android")
            pluginManager.apply("com.google.devtools.ksp")
            pluginManager.apply("com.google.dagger.hilt.android")
            extensions.configure<ApplicationExtension> {
                configureAndroid(this)
                defaultConfig.targetSdk = ProjectConfig.TARGET_SDK
            }
            configureCompose(extensions.getByType<ApplicationExtension>())
            dependencies.apply {
                add("implementation", libs.lib("androidx-core-ktx"))
                add("implementation", libs.lib("androidx-activity-compose"))
                add("implementation", libs.lib("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.lib("hilt-android"))
                add("ksp", libs.lib("hilt-compiler"))
            }
        }
    }
}
