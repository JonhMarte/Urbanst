import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.caracolas.gestortrainer.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("jvmLibrary") {
            id = "gestor.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("androidLibrary") {
            id = "gestor.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "gestor.android.library.compose"
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "gestor.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidApplication") {
            id = "gestor.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
    }
}
