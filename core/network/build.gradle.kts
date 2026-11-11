plugins {
    id("gestor.android.library")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.caracolas.gestortrainer.core.network"
}

dependencies {
    api(project(":core:model"))
    api(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
}
