plugins {
    id("gestor.android.feature")
}

android {
    namespace = "com.caracolas.gestortrainer.feature.clients.impl"
}

dependencies {
    api(project(":feature:clients:api"))
    implementation(project(":feature:profile:api"))
    implementation(project(":core:auth"))
    implementation(project(":core:events"))
    testImplementation(libs.kotlinx.coroutines.test)
}
