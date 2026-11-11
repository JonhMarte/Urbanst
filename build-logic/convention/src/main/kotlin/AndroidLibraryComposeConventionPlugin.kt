import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

/** Módulo Android con Jetpack Compose (contratos de UI, design system). */
class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("gestor.android.library")
            configureCompose(extensions.getByType<LibraryExtension>())
        }
    }
}
