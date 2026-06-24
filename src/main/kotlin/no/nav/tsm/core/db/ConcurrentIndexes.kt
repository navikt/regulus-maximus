package no.nav.tsm.core.db

import no.nav.tsm.core.PostgresConfig
import no.nav.tsm.core.logger
import java.sql.DriverManager

private val logger = logger()

fun runConcurrentIndexes(postgresConfig: PostgresConfig) {

    logger.info("Creating index concurrently")
    val r  = DriverManager.getConnection(
        postgresConfig.jdbc,
        postgresConfig.username,
        postgresConfig.password,
    )
        .use { connection ->
            connection.autoCommit = true // no transaction block for creating index concurrently
            connection.createStatement().use { statement ->
                statement.execute("CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_pasient_ident ON sykmelding(pasient_ident)")
            }
        }

    logger.info("Indexes created successfully = $r")
}
