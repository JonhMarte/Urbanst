plugins {
    id("gestor.jvm.library")
}

dependencies {
    api(project(":core:model"))
    testImplementation(kotlin("test"))
}
