plugins {
    id("gestor.jvm.library")
}

dependencies {
    api(project(":core:model"))
    api(project(":feature:calendar:api"))
    testImplementation(kotlin("test"))
}
