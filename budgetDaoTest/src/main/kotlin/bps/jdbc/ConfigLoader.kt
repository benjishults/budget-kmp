package bps.jdbc

import bps.config.ConfigurationHelper
import io.github.nhubbard.konf.Config
import io.github.nhubbard.konf.toValue

fun getConfigFromResource(fileName: String): Pair<JdbcConfig, HikariYamlConfig> =
    ConfigurationHelper(sequenceOf(fileName))
        .config
        .let { conf: Config ->
            conf.at("jdbc").toValue() as JdbcConfig to
                    try {
                        conf.at("hikari").toValue()
                    } catch (_: Exception) {
                        HikariYamlConfig()
                    }
        }
