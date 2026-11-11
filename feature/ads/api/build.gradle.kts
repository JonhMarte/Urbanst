plugins {
    id("gestor.android.library.compose")
}

android {
    namespace = "com.caracolas.gestortrainer.feature.ads.api"
}

dependencies {
    api(project(":core:model"))
}
