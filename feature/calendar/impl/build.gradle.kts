plugins {
    id("gestor.android.feature")
}

android {
    namespace = "com.caracolas.gestortrainer.feature.calendar.impl"
}

dependencies {
    api(project(":feature:calendar:api"))
    implementation(project(":feature:activities:api"))
    implementation(project(":feature:profile:api"))
    implementation(project(":feature:clients:api"))
    implementation(project(":core:events"))
    testImplementation(libs.kotlinx.coroutines.test)
}
