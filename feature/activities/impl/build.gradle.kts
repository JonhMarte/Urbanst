plugins {
    id("gestor.android.feature")
}

android {
    namespace = "com.caracolas.gestortrainer.feature.activities.impl"
}

dependencies {
    api(project(":feature:activities:api"))
    implementation(project(":feature:muscles:api"))
    implementation(project(":feature:media:api"))
    implementation(project(":core:auth"))
    testImplementation(libs.kotlinx.coroutines.test)
}
