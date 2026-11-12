plugins {
    id("gestor.android.feature")
}

android {
    namespace = "com.caracolas.gestortrainer.feature.payments.impl"
}

dependencies {
    api(project(":feature:payments:api"))
    implementation(project(":core:events"))
    implementation(project(":feature:clients:api"))
    implementation(project(":feature:profile:api"))
    testImplementation(libs.kotlinx.coroutines.test)
}
