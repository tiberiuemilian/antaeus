package io.pleo.antaeus.core.config

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec
import com.uchuhimo.konf.source.yaml

object ServerConfig : ConfigSpec("server") {
    val port by required<Int>()
}

object Configuration {
    val config = Config {
        addSpec(ServerConfig)
//        addSpec(DomainConfig)
    }
        .from.yaml.resource("config.yaml")
        .from.env()
        .from.systemProperties()
}
