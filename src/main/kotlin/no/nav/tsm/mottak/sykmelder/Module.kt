package no.nav.tsm.mottak.sykmelder

import io.ktor.server.application.Application

fun Application.configureSykmelderModule() {
    configureSykmelderDependencies()
}
