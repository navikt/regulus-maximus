package no.nav.tsm

import io.ktor.server.application.*
import no.nav.tsm.admin.configureAdminModule
import no.nav.tsm.mottak.configureMottakModule
import no.nav.tsm.plugins.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    // Global configuration
    configureDependencies()
    configureMonitoring()
    configureDatabase()
    configureSerialization()
    configureAuthentication()

    // Specific modules
    configureMottakModule()
    configureAdminModule()
}
