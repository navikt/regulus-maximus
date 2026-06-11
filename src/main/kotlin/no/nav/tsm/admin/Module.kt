package no.nav.tsm.admin

import io.ktor.server.application.*

fun Application.configureAdminModule() {
    configureAdminDependencies()
    configureAdminRoutes()
}
