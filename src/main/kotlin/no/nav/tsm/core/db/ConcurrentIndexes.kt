package no.nav.tsm.core.db

import java.sql.DriverManager
import no.nav.tsm.core.PostgresConfig
import no.nav.tsm.core.logger

private val logger = logger()
private const val INDEX_ADVISORY_LOCK_KEY = 136918763L

fun runConcurrentIndexes(postgresConfig: PostgresConfig) {
    DriverManager.getConnection(
            postgresConfig.jdbc,
            postgresConfig.username,
            postgresConfig.password,
        )
        .use { connection ->
            connection.autoCommit = true

            val gotLock =
                connection.prepareStatement("SELECT pg_try_advisory_lock(?)").use { statement ->
                    statement.setLong(1, INDEX_ADVISORY_LOCK_KEY)
                    statement.executeQuery().use { rs -> rs.next() && rs.getBoolean(1) }
                }
            if (!gotLock) {
                logger.info("Another pod holds the index advisory lock, skipping")
                return
            }

            try {
                logger.info("Creating index concurrently")
                connection.createStatement().use { statement ->
                    statement.execute(
                        "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_pasient_ident ON sykmelding(pasient_ident)"
                    )
                }
                logger.info("Indexes created successfully")
            } finally {
                connection.prepareStatement("SELECT pg_advisory_unlock(?)").use { statement ->
                    statement.setLong(1, INDEX_ADVISORY_LOCK_KEY)
                    statement.execute()
                }
            }
        }
}
