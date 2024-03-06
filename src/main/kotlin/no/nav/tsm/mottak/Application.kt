package no.nav.tsm.mottak

import io.ktor.server.application.*
import io.ktor.server.netty.*
import no.nav.tsm.mottak.plugins.*
import org.koin.ktor.ext.get

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureDependencyInjection()
    configureMonitoring()
    configureSerialization()
    configureDatabases(get())
    configureRouting()
    configureConsumer(get())
}
