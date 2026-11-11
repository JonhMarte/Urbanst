plugins {
    id("gestor.android.feature")
}

android {
    namespace = "com.caracolas.gestortrainer.feature.payments.impl"
}

dependencies {
    api(project(":feature:payments:api"))
}
