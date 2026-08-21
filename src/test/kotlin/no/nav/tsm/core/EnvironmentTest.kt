package no.nav.tsm.core

import com.typesafe.config.ConfigFactory
import io.kotest.matchers.equals.shouldEqual
import io.ktor.server.config.*
import kotlin.test.Test

class EnvironmentTest {
    @Test
    fun `production environment should be properly configured, even lazy values`() {
        val applicationConfig =
            HoconApplicationConfig(
                ConfigFactory.parseMap(
                        mapOf(
                            // Nais injected values
                            "NAIS_POD_NAME" to "regulus-maximus-prod-123",
                            "DB_JDBC_URL" to "jdbc:postgresql://db-host:5432/sykinn",
                            "DB_HOST" to "db-host",
                            "DB_PORT" to "5432",
                            "DB_DATABASE" to "sykinn",
                            "DB_SSLROOTCERT" to "/var/run/secrets/db/ca.pem",
                            "DB_SSLCERT" to "/var/run/secrets/db/client-cert.pem",
                            "DB_SSLKEY_PK8" to "/var/run/secrets/db/client-key.pk8",
                            "DB_USERNAME" to "db-user",
                            "DB_PASSWORD" to "db-password",
                            "BEHANDLINGSDAGER_IDS" to "123,123,123",
                        )
                    )
                    .withFallback(ConfigFactory.parseResources("application.conf"))
                    .resolve()
            )

        val environment = initializeEnvironment(applicationConfig)

        // Poke lazy envs as well to ensure they are properly configured
        environment.runtime.name shouldEqual "regulus-maximus-prod-123"
    }
}
