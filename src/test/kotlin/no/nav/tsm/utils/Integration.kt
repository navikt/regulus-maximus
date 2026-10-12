package no.nav.tsm.utils

import no.nav.tsm.core.db.runConcurrentIndexes
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.testcontainers.postgresql.PostgreSQLContainer

abstract class WithPostgresql {
    companion object {
        val postgres =
            PostgreSQLContainer("postgres:17-alpine").apply {
                withInitScript("db/test_init.sql")
                start()
            }

        val config = createIntegrationEnvironment(postgres)

        fun runMigrations(clean: Boolean = false) {
            val flyway =
                Flyway.configure()
                    .dataSource(
                        config.postgres.jdbc,
                        config.postgres.username,
                        config.postgres.password,
                    )
                    .cleanDisabled(false)
                    .locations("db/migration")
                    .load()

            if (clean) {
                flyway.clean()
            }
            flyway.migrate()
            runConcurrentIndexes(config.postgres)
        }

        fun connect() {
            Database.connect(
                url = config.postgres.jdbc,
                user = config.postgres.username,
                password = config.postgres.password,
            )
        }
    }
}
