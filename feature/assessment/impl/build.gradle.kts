plugins {
    id("gestor.android.feature")
}

android {
    namespace = "com.caracolas.gestortrainer.feature.assessment.impl"
}

dependencies {
    api(project(":feature:assessment:api"))
    implementation(project(":core:events"))
    implementation(project(":feature:muscles:api"))
    implementation(project(":feature:profile:api"))
    testImplementation(libs.kotlinx.coroutines.test)
}
