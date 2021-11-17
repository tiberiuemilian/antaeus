plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    api(project(":pleo-antaeus-models"))
    implementation("com.uchuhimo:konf:1.1.2")
}
