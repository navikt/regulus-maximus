package no.nav.tsm.mottak.plugins

import io.ktor.server.application.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.tsm.mottak.sykmelding.kafka.SykmeldingConsumer

fun Application.configureConsumer(sykmeldingConsumer: SykmeldingConsumer) {
    launch(Dispatchers.IO) { sykmeldingConsumer.consumeSykmelding() }
}
