package no.nav.tsm.utils

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson3.jackson
import io.ktor.server.testing.*
import no.nav.tsm.sykmelding.input.core.model.SykmeldingModule

fun ApplicationTestBuilder.testClient(): HttpClient {
    return createClient {
        install(ContentNegotiation) { jackson { addModules(SykmeldingModule()) } }
    }
}
