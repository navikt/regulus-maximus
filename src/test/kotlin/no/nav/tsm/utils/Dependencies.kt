package no.nav.tsm.utils

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import no.nav.tsm.core.Environment
import no.nav.tsm.core.PostgresConfig
import no.nav.tsm.core.PostgresR2DBCConfig
import no.nav.tsm.core.Runtime
import no.nav.tsm.ktor.nais.RuntimeCluster
import no.nav.tsm.module
import no.nav.tsm.plugins.configureAuthentication
import no.nav.tsm.plugins.configureDependencies
import no.nav.tsm.plugins.configureSerialization
import org.testcontainers.kafka.ConfluentKafkaContainer
import org.testcontainers.postgresql.PostgreSQLContainer

fun Application.configurePostgresIntegrationTests(postgres: PostgreSQLContainer) {
    // Integration test specific Environment configuration
    dependencies { provide<Environment>() { createIntegrationEnvironment(postgres, null) } }

    // Global
    configureAuthentication()
    configureDependencies()
    configureSerialization()

    // #1: Postgres specific tests will have to provide their own "in test" set of modules
}

fun Application.configureFullIntegrationTests(
    postgres: PostgreSQLContainer,
    kafka: ConfluentKafkaContainer,
) {
    // Integration test specific Environment configuration
    dependencies { provide<Environment>() { createIntegrationEnvironment(postgres, kafka) } }

    // #2: Postgresql + Kafka tests just set up the entire application
    module()
}

fun createIntegrationEnvironment(postgres: PostgreSQLContainer, kafka: ConfluentKafkaContainer?) =
    Environment(
        runtime = Runtime(env = RuntimeCluster.LOCAL, name = "test-app"),
        postgres =
            PostgresConfig(
                jdbc = postgres.jdbcUrl,
                r2 =
                    PostgresR2DBCConfig(
                        url = "r2dbc:${postgres.jdbcUrl.removePrefix("jdbc:")}",
                        sslCert = null,
                        sslKeyPk8 = null,
                    ),
                username = postgres.username,
                password = postgres.password,
            ),
        behandlingsdagerIds = emptyList(),
    )
