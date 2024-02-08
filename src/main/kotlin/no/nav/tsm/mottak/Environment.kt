package no.nav.tsm.mottak

import io.ktor.server.application.*
import io.ktor.server.config.*

class Environment(
    val jdbcUrl: String,
    val dbUser: String,
    val dbPassword: String,
)

object EnvironmentSingleton {
  private var instance: Environment? = null

  fun instance(config: ApplicationConfig): Environment {
    if (instance != null) {
      return instance ?: throw IllegalStateException("Environment not initialized")
    }

    return Environment(
            jdbcUrl = "jdbc:${config.requiredEnv("ktor.database.dbUrl")}",
            dbUser = config.requiredEnv("ktor.database.dbUser"),
            dbPassword = config.requiredEnv("ktor.database.dbPassword"),
        )
        .also { instance = it }
  }

  private fun ApplicationConfig.requiredEnv(name: String) =
      propertyOrNull(name)?.getString()
          ?: throw IllegalArgumentException("Missing required environment variable $name")
}

val ApplicationEnvironment.env: Environment
  get() = EnvironmentSingleton.instance(this.config)
