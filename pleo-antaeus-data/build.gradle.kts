plugins {
    kotlin("jvm")
}

kotlinProject()

dataLibs()

dependencies {
    implementation(project(":pleo-antaeus-config"))
    api(project(":pleo-antaeus-models"))
    implementation("com.uchuhimo:konf:1.1.2")
}
