package no.nav.tsm.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.tsm.core.Environment
import no.nav.tsm.core.db.getFlyway
import no.nav.tsm.core.db.runConcurrentIndexes
import no.nav.tsm.ktor.logger
import org.jetbrains.exposed.v1.jdbc.Database

private val logger = logger()

fun Application.configureDatabase() {
    val env: Environment by dependencies

    getFlyway(env.postgres).migrate()

    Database.connect(
        url = env.postgres.jdbc,
        user = env.postgres.username,
        password = env.postgres.password,
    )

    monitor.subscribe(ApplicationStarted) {
        launch(Dispatchers.IO) {
            try {
                runConcurrentIndexes(env.postgres)
            } catch (e: Exception) {
                logger.error("Failed to create concurrent indexes", e)
            }
        }
    }
}
