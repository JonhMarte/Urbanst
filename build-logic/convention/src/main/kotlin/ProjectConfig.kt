import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/** Valores comunes del proyecto. */
object ProjectConfig {
    const val COMPILE_SDK = 36
    const val MIN_SDK = 26
    const val TARGET_SDK = 36
}

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.lib(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).orElseThrow { IllegalStateException("Falta '$alias' en libs.versions.toml") }

internal fun Project.configureAndroid(extension: CommonExtension<*, *, *, *, *, *>) {
    extension.apply {
        compileSdk = ProjectConfig.COMPILE_SDK
        defaultConfig {
            minSdk = ProjectConfig.MIN_SDK
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
    configureKotlinJvmTarget()
}

internal fun Project.configureKotlinJvmTarget() {
    // Muchos módulos se llaman "api" o "impl": un nombre de módulo Kotlin único evita choques
    // de META-INF/*.kotlin_module al empaquetar la app.
    val uniqueModuleName = path.removePrefix(":").replace(':', '-')
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            moduleName.set(uniqueModuleName)
        }
    }
}

internal fun Project.configureCompose(extension: CommonExtension<*, *, *, *, *, *>) {
    pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
    extension.buildFeatures.compose = true
    dependencies.apply {
        val bom = libs.lib("compose-bom")
        add("implementation", platform(bom))
        add("implementation", libs.lib("compose-ui"))
        add("implementation", libs.lib("compose-material3"))
        add("implementation", libs.lib("compose-ui-tooling-preview"))
        add("debugImplementation", libs.lib("compose-ui-tooling"))
    }
}
