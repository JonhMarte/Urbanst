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
    implementation(project(":feature:reviews:api"))
    implementation(project(":feature:leaderboards:api"))
    implementation(project(":feature:scoring:api"))
    implementation(project(":feature:calendar:api"))
    implementation(project(":feature:messaging:api"))
    implementation(project(":core:auth"))
    testImplementation(libs.kotlinx.coroutines.test)
}
