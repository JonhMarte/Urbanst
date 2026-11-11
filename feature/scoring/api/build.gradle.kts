plugins {
    id("gestor.jvm.library")
}

dependencies {
    api(project(":core:model"))
    api(project(":core:events"))
    testImplementation(kotlin("test"))
}
