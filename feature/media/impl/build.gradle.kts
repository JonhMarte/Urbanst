plugins {
    id("gestor.android.feature")
}

android {
    namespace = "com.caracolas.gestortrainer.feature.media.impl"
}

dependencies {
    api(project(":feature:media:api"))
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
}
