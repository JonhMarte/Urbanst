plugins {
    id("gestor.android.feature")
}

android {
    namespace = "com.caracolas.gestortrainer.feature.groups.stub"
}

dependencies {
    api(project(":feature:groups:api"))
}
