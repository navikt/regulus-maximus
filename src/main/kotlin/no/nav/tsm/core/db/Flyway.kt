package no.nav.tsm.core.db

import no.nav.tsm.core.PostgresConfig
import org.flywaydb.core.Flyway

fun getFlyway(postgresConfig: PostgresConfig): Flyway =
    Flyway.configure()
        .dataSource(postgresConfig.jdbc, postgresConfig.username, postgresConfig.password)
        .locations("db/migration")
        .load()
