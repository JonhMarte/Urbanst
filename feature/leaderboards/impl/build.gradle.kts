plugins {
    id("gestor.android.feature")
}

android {
    namespace = "com.caracolas.gestortrainer.feature.leaderboards.impl"
}

dependencies {
    api(project(":feature:leaderboards:api"))
    implementation(project(":feature:scoring:api"))
    implementation(project(":feature:clients:api"))
    implementation(project(":feature:profile:api"))
    implementation(project(":feature:calendar:api"))
    implementation(project(":feature:reviews:api"))
    testImplementation(libs.kotlinx.coroutines.test)
}
