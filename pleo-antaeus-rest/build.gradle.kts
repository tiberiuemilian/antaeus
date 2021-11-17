plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-config"))
    implementation(project(":pleo-antaeus-core"))
    implementation(project(":pleo-antaeus-models"))

    implementation("io.javalin:javalin:4.1.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.10.3")

    implementation("mysql:mysql-connector-java:8.0.25")
    implementation("com.uchuhimo:konf:1.1.2")
}
