package no.nav.tsm.core

import io.ktor.server.config.*
import no.nav.tsm.ktor.nais.RuntimeCluster
import no.nav.tsm.ktor.nais.getRuntimeCluster

class Runtime(val env: RuntimeCluster, val name: String)

class ExternalApi(val tsmBehandler: String)

class PostgresConfig(val jdbc: String, val username: String, val password: String)

class Environment(
    val runtime: Runtime,
    val postgres: PostgresConfig,
    val behandlingsdagerIds: List<String>,
    val external: () -> ExternalApi,
)

fun initializeEnvironment(config: ApplicationConfig): Environment {
    return Environment(
        runtime =
            Runtime(env = getRuntimeCluster(), name = config.property("app.name").getString()),
        postgres =
            PostgresConfig(
                jdbc = config.property("postgres.jdbc").getString(),
                username = config.property("postgres.username").getString(),
                password = config.property("postgres.password").getString(),
            ),
        behandlingsdagerIds =
            config.property("behandlingsdager.ids").getString().split(',').filter {
                it.isNotEmpty()
            },
        external = {
            ExternalApi(tsmBehandler = config.property("external.tsmBehandler").getString())
        },
    )
}
