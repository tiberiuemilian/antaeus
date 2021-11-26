plugins {
    kotlin("jvm")
}

kotlinProject()

dataLibs()

dependencies {
    implementation(project(":pleo-antaeus-config"))
    api(project(":pleo-antaeus-models"))
    implementation("com.uchuhimo:konf:1.1.2")
    implementation( platform("org.testcontainers:testcontainers-bom:1.16.2")) //import bom
    testImplementation("org.testcontainers:mysql") //no version specified
    testImplementation("com.h2database:h2:1.4.199")
}
