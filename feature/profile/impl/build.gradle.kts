plugins {
    id("gestor.android.feature")
}

android {
    namespace = "com.caracolas.gestortrainer.feature.profile.impl"
}

dependencies {
    api(project(":feature:profile:api"))
    implementation(project(":core:auth"))
}
