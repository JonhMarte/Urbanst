plugins {
    id("gestor.jvm.library")
}

dependencies {
    api(project(":core:model"))
    api(project(":feature:payments:api"))
    testImplementation(kotlin("test"))
}
