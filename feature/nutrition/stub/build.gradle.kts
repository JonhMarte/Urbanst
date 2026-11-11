plugins {
    id("gestor.android.feature")
}

android {
    namespace = "com.caracolas.gestortrainer.feature.nutrition.stub"
}

dependencies {
    api(project(":feature:nutrition:api"))
}
