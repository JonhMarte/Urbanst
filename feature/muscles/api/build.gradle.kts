plugins {
    id("gestor.android.library.compose")
}

android {
    namespace = "com.caracolas.gestortrainer.feature.muscles.api"
}

dependencies {
    api(project(":core:model"))
}
