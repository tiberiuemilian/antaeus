plugins {
    application
    kotlin("jvm")
    id("com.google.cloud.tools.jib") version "3.1.4"
}

kotlinProject()

dataLibs()

application {
    mainClassName = "io.pleo.antaeus.app.AntaeusApp"
}

dependencies {
    implementation(project(":pleo-antaeus-config"))
    implementation(project(":pleo-antaeus-data"))
    implementation(project(":pleo-antaeus-rest"))
    implementation(project(":pleo-antaeus-core"))
    implementation(project(":pleo-antaeus-models"))
    implementation("com.uchuhimo:konf:1.1.2")
    implementation(platform("org.jetbrains.kotlin:kotlin-reflect:1.5.31"))
}

jib {
    to {
        val imageName = System.getenv("IMAGE_NAME")
        image = imageName ?: "antaeus"
    }

    container {
        mainClass = "io.pleo.antaeus.app.AntaeusAppKt"
        environment = mapOf(
                        Pair("agent.name", "UNKNOWN"),
                        Pair("agent.port", "7070"))

        jvmFlags = listOf(
            "-server",
            "-Djava.awt.headless=true",
            "-XX:InitialRAMFraction=2",
            "-XX:MinRAMFraction=2",
            "-XX:MaxRAMFraction=2",
            "-XX:+UseG1GC",
            "-XX:MaxGCPauseMillis=100",
            "-XX:+UseStringDeduplication"
        )
    }
}
