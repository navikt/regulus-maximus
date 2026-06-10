package no.nav.tsm.mottak

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.tsm.mottak.sykmelding.kafka.SykmeldingInputConsumerService

fun Application.configureMottakModule() {
    configureMottakDependencies()
    configureConsumer()
}

fun Application.configureConsumer() {
    val consumer: SykmeldingInputConsumerService by dependencies

    monitor.subscribe(ApplicationStarted) { launch(Dispatchers.IO) { consumer.consumeWithRetry() } }
}
