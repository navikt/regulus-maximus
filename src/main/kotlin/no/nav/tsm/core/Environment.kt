package no.nav.tsm.core

import io.ktor.server.config.*
import no.nav.tsm.ktor.nais.RuntimeCluster
import no.nav.tsm.ktor.nais.getRuntimeCluster

class Runtime(val env: RuntimeCluster, val name: String)

class PostgresR2DBCConfig(val url: String, val sslCert: String?, val sslKeyPk8: String?)

class PostgresConfig(
    val jdbc: String,
    val r2: PostgresR2DBCConfig,
    val username: String,
    val password: String,
)

class Environment(
    val runtime: Runtime,
    val postgres: PostgresConfig,
    val behandlingsdagerIds: List<String>,
)

fun initializeEnvironment(config: ApplicationConfig): Environment {
    return Environment(
        runtime =
            Runtime(env = getRuntimeCluster(), name = config.property("app.name").getString()),
        postgres =
            PostgresConfig(
                jdbc = config.property("postgres.jdbc").getString(),
                r2 =
                    PostgresR2DBCConfig(
                        url = config.property("postgres.r2dbc.url").getString(),
                        sslCert = config.propertyOrNull("postgres.r2dbc.sslCert")?.getString(),
                        sslKeyPk8 = config.propertyOrNull("postgres.r2dbc.sslKeyPk8")?.getString(),
                    ),
                username = config.property("postgres.username").getString(),
                password = config.property("postgres.password").getString(),
            ),
        behandlingsdagerIds =
            config.property("behandlingsdager.ids").getString().split(',').filter {
                it.isNotEmpty()
            },
    )
}
