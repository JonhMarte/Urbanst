plugins {
    id("gestor.android.feature")
}

android {
    namespace = "com.caracolas.gestortrainer.feature.plan.impl"
}

dependencies {
    api(project(":feature:plan:api"))
    implementation(project(":feature:media:api"))
    implementation(project(":feature:activities:api"))
    implementation(project(":feature:clients:api"))
    implementation(project(":feature:tracking:api"))
    testImplementation(libs.kotlinx.coroutines.test)
}
