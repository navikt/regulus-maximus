package no.nav.tsm.mottak

import io.ktor.server.application.*
import io.ktor.server.config.*
import java.util.Properties

class Environment(
    val jdbcUrl: String,
    val dbUser: String,
    val dbPassword: String,
    val kafkaConfig: Properties
)

object EnvironmentSingleton {
    private var instance: Environment? = null

    fun instance(config: ApplicationConfig): Environment {
        if (instance != null) {
            return instance ?: throw IllegalStateException("Environment not initialized")
        }

        val host = config.requiredEnv("ktor.database.dbHost")
        val port = config.requiredEnv("ktor.database.dbPort")
        val database = config.requiredEnv("ktor.database.dbName")
        return Environment(
            jdbcUrl = "jdbc:postgresql://$host:$port/$database",
            dbUser = config.requiredEnv("ktor.database.dbUser"),
            dbPassword = config.requiredEnv("ktor.database.dbPassword"),
            kafkaConfig = Properties().apply {
                config.config("ktor.kafka.config").toMap().forEach {
                    this[it.key] = it.value
                }
            }
        )
            .also { instance = it }
    }

    private fun ApplicationConfig.requiredEnv(name: String) =
        propertyOrNull(name)?.getString()
            ?: throw IllegalArgumentException("Missing required environment variable $name")
}

val ApplicationEnvironment.env: Environment
    get() = EnvironmentSingleton.instance(this.config)
