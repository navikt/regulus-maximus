package no.nav.tsm.mottak.plugins

import io.ktor.server.application.Application
import io.ktor.server.config.ApplicationConfig
import java.util.Properties

class Environment(
    val jdbcUrl: String,
    val dbUser: String,
    val dbPassword: String,
    val kafkaConfig: Properties,
    val mottattSykmeldingTopic: String = "tsm.mottak-sykmelding",
    val sykmeldingMedUtfall: String = "tsm.mottak-sykmelding-med-utfall"
)
private fun ApplicationConfig.requiredEnv(name: String) =
    propertyOrNull(name)?.getString()
        ?: throw IllegalArgumentException("Missing required environment variable $name")
fun Application.createEnvironment(): Environment {
    val host = environment.config.requiredEnv("ktor.database.dbHost")
    val port = environment.config.requiredEnv("ktor.database.dbPort")
    val database = environment.config.requiredEnv("ktor.database.dbName")
    return Environment(
        jdbcUrl = "jdbc:postgresql://$host:$port/$database",
        dbUser = environment.config.requiredEnv("ktor.database.dbUser"),
        dbPassword = environment.config.requiredEnv("ktor.database.dbPassword"),
        kafkaConfig = Properties().apply {
            environment.config.config("ktor.kafka.config").toMap().forEach {
                this[it.key] = it.value
            }
        }
    )
}
