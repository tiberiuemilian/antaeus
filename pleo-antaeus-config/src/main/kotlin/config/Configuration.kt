package config

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.source.yaml

object AgentConfig : ConfigSpec("agent") {
    val port by required<Int>()
    val agentName by required<String>("name")
    val databaseHost by optional("db")
    val databasePoolSize by optional(10)
}

object AppConfig : ConfigSpec("app") {
    val batchSize by required<Int>()
    val delayBetweenBatches by optional(0L)
}

object Configuration {
    val config = Config {
        addSpec(AgentConfig)
        addSpec(AppConfig)
    }
        .from.yaml.resource("config.yaml")
        .from.env()
        .from.systemProperties()
}
