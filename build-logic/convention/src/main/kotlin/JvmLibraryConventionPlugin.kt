import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure

/** Módulo Kotlin puro (sin Android): modelos e interfaces de dominio. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("java-library")
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            configureKotlinJvmTarget()
            dependencies.add("api", libs.lib("kotlinx-coroutines-core"))
            dependencies.add("api", libs.lib("javax-inject"))
            dependencies.add("testImplementation", libs.lib("kotlinx-coroutines-test"))
            // Los tests usan kotlin.test / JUnit4 (único motor en el catálogo); el motor
            // de JUnit Platform (JUnit 5) no está presente y no encontraría los tests.
        }
    }
}
