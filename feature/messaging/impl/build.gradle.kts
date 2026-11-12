plugins {
    id("gestor.android.feature")
}

android {
    namespace = "com.caracolas.gestortrainer.feature.messaging.impl"
}

dependencies {
    api(project(":feature:messaging:api"))
    implementation(project(":core:events"))
    implementation(project(":feature:clients:api"))
    implementation(project(":feature:profile:api"))
    testImplementation(libs.kotlinx.coroutines.test)
}
