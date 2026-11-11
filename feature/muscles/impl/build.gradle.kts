plugins {
    id("gestor.android.feature")
}

android {
    namespace = "com.caracolas.gestortrainer.feature.muscles.impl"
}

dependencies {
    api(project(":feature:muscles:api"))
}
