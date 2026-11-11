plugins {
    id("gestor.android.feature")
}

android {
    namespace = "com.caracolas.gestortrainer.feature.tracking.impl"
}

dependencies {
    api(project(":feature:tracking:api"))
    implementation(project(":core:events"))
    implementation(project(":feature:calendar:api"))
    testImplementation(libs.kotlinx.coroutines.test)
}
