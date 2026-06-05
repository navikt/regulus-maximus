package no.nav.tsm.mottak

import io.ktor.server.application.Application

fun Application.configureMottakModule() {
    configureMottakDependencies()
}
