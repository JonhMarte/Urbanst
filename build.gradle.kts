// Plugins declarados aquí (sin aplicar) para que todos los módulos usen la misma versión.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

// Regla de arquitectura: un módulo solo puede depender del :api de otro.
// Solo :app puede depender de :impl o :stub. Si se rompe, el build falla.
subprojects {
    afterEvaluate {
        if (path == ":app") return@afterEvaluate
        configurations.forEach { conf ->
            conf.dependencies.withType(ProjectDependency::class.java).forEach { dep ->
                val target = dep.path
                if (target.endsWith(":impl") || target.endsWith(":stub")) {
                    throw GradleException(
                        "Regla de arquitectura rota: $path depende de $target. " +
                            "Depende de su módulo :api y deja que :app conecte la implementación."
                    )
                }
            }
        }
    }
}
