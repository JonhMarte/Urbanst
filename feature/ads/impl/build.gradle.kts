plugins {
    id("gestor.android.feature")
}

android {
    namespace = "com.caracolas.gestortrainer.feature.ads.impl"
}

dependencies {
    api(project(":feature:ads:api"))
    implementation(project(":feature:payments:api"))
}
