plugins {
    id("gestor.android.feature")
}

android {
    namespace = "com.caracolas.gestortrainer.feature.reviews.impl"
}

dependencies {
    api(project(":feature:reviews:api"))
    implementation(project(":core:events"))
    implementation(project(":feature:clients:api"))
    implementation(project(":feature:profile:api"))
    testImplementation(libs.kotlinx.coroutines.test)
}
