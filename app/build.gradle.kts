plugins {
    id("gestor.android.application")
}

android {
    namespace = "com.caracolas.gestortrainer"
    defaultConfig {
        applicationId = "com.caracolas.gestortrainer"
        versionCode = 1
        versionName = "0.1.0"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // Núcleo
    implementation(project(":core:model"))
    implementation(project(":core:events"))
    implementation(project(":core:featureflags"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:auth"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))

    // Contratos (api)
    implementation(project(":feature:onboarding:api"))
    implementation(project(":feature:profile:api"))
    implementation(project(":feature:activities:api"))
    implementation(project(":feature:muscles:api"))
    implementation(project(":feature:media:api"))
    implementation(project(":feature:clients:api"))
    implementation(project(":feature:calendar:api"))
    implementation(project(":feature:plan:api"))
    implementation(project(":feature:tracking:api"))
    implementation(project(":feature:scoring:api"))
    implementation(project(":feature:leaderboards:api"))
    implementation(project(":feature:reviews:api"))
    implementation(project(":feature:discovery:api"))
    implementation(project(":feature:assessment:api"))
    implementation(project(":feature:messaging:api"))
    implementation(project(":feature:payments:api"))
    implementation(project(":feature:matching:api"))
    implementation(project(":feature:ads:api"))
    implementation(project(":feature:notifications:api"))
    implementation(project(":feature:nutrition:api"))
    implementation(project(":feature:groups:api"))

    // Implementaciones: solo :app puede verlas
    implementation(project(":feature:onboarding:impl"))
    implementation(project(":feature:profile:impl"))
    implementation(project(":feature:activities:impl"))
    implementation(project(":feature:muscles:impl"))
    implementation(project(":feature:media:impl"))
    implementation(project(":feature:clients:impl"))
    implementation(project(":feature:calendar:impl"))
    implementation(project(":feature:plan:impl"))
    implementation(project(":feature:tracking:impl"))
    implementation(project(":feature:scoring:impl"))
    implementation(project(":feature:leaderboards:impl"))
    implementation(project(":feature:reviews:impl"))
    implementation(project(":feature:discovery:impl"))
    implementation(project(":feature:assessment:impl"))
    implementation(project(":feature:messaging:impl"))
    implementation(project(":feature:payments:impl"))
    implementation(project(":feature:matching:impl"))
    implementation(project(":feature:ads:impl"))
    implementation(project(":feature:notifications:impl"))
    implementation(project(":feature:nutrition:stub"))
    implementation(project(":feature:groups:stub"))
}
