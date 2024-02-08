package no.nav.tsm.mottak.plugins

import no.nav.tsm.mottak.env
import io.ktor.server.application.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.*


fun Application.configureDatabases() {
    Flyway.configure()
        .dataSource(environment.env.jdbcUrl, environment.env.dbUser, environment.env.dbPassword)
        .validateMigrationNaming(true)
        .load()
        .migrate()

    Database.connect(
        url = environment.env.jdbcUrl,
        user = environment.env.dbUser,
        password = environment.env.dbPassword,
        driver = "org.postgresql.Driver",
    )
}
