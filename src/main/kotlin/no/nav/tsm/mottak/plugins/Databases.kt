package no.nav.tsm.mottak.plugins

import io.ktor.server.application.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.*


fun Application.configureDatabases(environment: Environment) {
    Flyway.configure()
        .dataSource(environment.jdbcUrl, environment.dbUser, environment.dbPassword)
        .validateMigrationNaming(true)
        .load()
        .migrate()

    Database.connect(
        url = environment.jdbcUrl,
        user = environment.dbUser,
        password = environment.dbPassword,
        driver = "org.postgresql.Driver",
    )
}
