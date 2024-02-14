package no.nav.tsm.mottak

import io.ktor.server.application.*
import io.ktor.server.netty.*
import no.nav.tsm.mottak.plugins.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureMonitoring()
    configureSerialization()
    configureDatabases()
    configureRouting()
    configureConsumer()
}
