plugins {
    id("gestor.jvm.library")
}

dependencies {
    api(project(":core:model"))
    api(project(":feature:profile:api"))
    testImplementation(kotlin("test"))
}
