package no.nav.tsm.admin

import io.ktor.server.application.Application

fun Application.configureAdminModule() {
    configureAdminDependencies()
    configureAdminRoutes()
}
