plugins {
    id("gestor.android.library")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.caracolas.gestortrainer.core.database"
}

dependencies {
    api(project(":core:model"))
    api(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
