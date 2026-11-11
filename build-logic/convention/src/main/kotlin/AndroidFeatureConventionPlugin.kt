import org.gradle.api.Plugin
import org.gradle.api.Project

/** Implementación de una funcionalidad: Compose + Hilt + ViewModel + design system. */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("gestor.android.library.compose")
            pluginManager.apply("com.google.devtools.ksp")
            pluginManager.apply("com.google.dagger.hilt.android")
            dependencies.apply {
                add("implementation", project(":core:model"))
                add("implementation", project(":core:designsystem"))
                add("implementation", project(":core:featureflags"))
                add("implementation", libs.lib("hilt-android"))
                add("ksp", libs.lib("hilt-compiler"))
                add("implementation", libs.lib("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.lib("androidx-lifecycle-viewmodel-compose"))
            }
        }
    }
}
