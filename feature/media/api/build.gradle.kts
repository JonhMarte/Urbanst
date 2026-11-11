plugins {
    id("gestor.android.library.compose")
}

android {
    namespace = "com.caracolas.gestortrainer.feature.media.api"
}

dependencies {
    api(project(":core:model"))
}
