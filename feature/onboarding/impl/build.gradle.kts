plugins {
    id("gestor.android.feature")
}

android {
    namespace = "com.caracolas.gestortrainer.feature.onboarding.impl"
}

dependencies {
    api(project(":feature:onboarding:api"))
    implementation(project(":core:auth"))
}
