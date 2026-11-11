plugins {
    id("gestor.android.feature")
}

android {
    namespace = "com.caracolas.gestortrainer.feature.notifications.impl"
}

dependencies {
    api(project(":feature:notifications:api"))
}
