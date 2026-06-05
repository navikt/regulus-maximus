package no.nav.tsm.utils

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import no.nav.tsm.sykmelding.input.core.model.SykmeldingModule

fun ApplicationTestBuilder.testClient(): HttpClient {
    return createClient {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                registerModule(SykmeldingModule())
            }
        }
    }
}
