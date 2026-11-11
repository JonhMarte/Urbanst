plugins {
    id("gestor.android.feature")
}

android {
    namespace = "com.caracolas.gestortrainer.feature.scoring.impl"
}

dependencies {
    api(project(":feature:scoring:api"))
    implementation(project(":core:events"))
    implementation(project(":feature:calendar:api"))
    implementation(project(":feature:activities:api"))
    testImplementation(libs.kotlinx.coroutines.test)
}
