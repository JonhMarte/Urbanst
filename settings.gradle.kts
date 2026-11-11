pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "GestorTrainer"

include(":app")
include(":core:model")
include(":core:events")
include(":core:featureflags")
include(":core:designsystem")
include(":core:auth")
include(":core:network")
include(":core:database")
include(":feature:onboarding:api")
include(":feature:onboarding:impl")
include(":feature:profile:api")
include(":feature:profile:impl")
include(":feature:activities:api")
include(":feature:activities:impl")
include(":feature:muscles:api")
include(":feature:muscles:impl")
include(":feature:media:api")
include(":feature:media:impl")
include(":feature:clients:api")
include(":feature:clients:impl")
include(":feature:calendar:api")
include(":feature:calendar:impl")
include(":feature:plan:api")
include(":feature:plan:impl")
include(":feature:tracking:api")
include(":feature:tracking:impl")
include(":feature:scoring:api")
include(":feature:scoring:impl")
include(":feature:leaderboards:api")
include(":feature:leaderboards:impl")
include(":feature:reviews:api")
include(":feature:reviews:impl")
include(":feature:discovery:api")
include(":feature:discovery:impl")
include(":feature:payments:api")
include(":feature:payments:impl")
include(":feature:ads:api")
include(":feature:ads:impl")
include(":feature:notifications:api")
include(":feature:notifications:impl")
include(":feature:nutrition:api")
include(":feature:nutrition:stub")
include(":feature:groups:api")
include(":feature:groups:stub")

// FUTURO: integrations/viewer3d (Unity as a Library). No se incluye en el MVP.
