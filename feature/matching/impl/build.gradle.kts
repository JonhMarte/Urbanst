plugins {
    id("gestor.android.feature")
}

android {
    namespace = "com.caracolas.gestortrainer.feature.matching.impl"
}

dependencies {
    api(project(":feature:matching:api"))
    implementation(project(":core:events"))
    implementation(project(":feature:payments:api"))
    implementation(project(":feature:profile:api"))
    implementation(project(":feature:reviews:api"))
    testImplementation(libs.kotlinx.coroutines.test)
}
