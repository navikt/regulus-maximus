package no.nav.tsm.mottak

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.plugins.di.dependencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.nav.tsm.mottak.sykmelding.kafka.SykmeldingInputConsumerService

fun Application.configureMottakModule() {
    configureMottakDependencies()
    configureConsumer()
}

fun Application.configureConsumer() {
    val consumer: SykmeldingInputConsumerService by dependencies

    var job: Job? = null
    monitor.subscribe(ApplicationStarted) { job = launch(Dispatchers.IO) { consumer.consume() } }

    monitor.subscribe(ApplicationStopping) {
        launch { withContext(NonCancellable) { job?.cancelAndJoin() } }
    }
}
