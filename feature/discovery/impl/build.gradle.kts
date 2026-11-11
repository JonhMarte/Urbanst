plugins {
    id("gestor.android.feature")
}

android {
    namespace = "com.caracolas.gestortrainer.feature.discovery.impl"
}

dependencies {
    api(project(":feature:discovery:api"))
    implementation(project(":feature:clients:api"))
    implementation(project(":feature:profile:api"))
    implementation(project(":core:auth"))
}
