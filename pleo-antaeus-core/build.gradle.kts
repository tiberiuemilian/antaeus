plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-config"))
    implementation(project(":pleo-antaeus-data"))
    api(project(":pleo-antaeus-models"))
    implementation("com.uchuhimo:konf:1.1.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.2")
}
