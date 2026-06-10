package no.nav.tsm.admin

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.plugins.di.dependencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.tsm.admin.datefixer.DateFixerRepo
import no.nav.tsm.admin.datefixer.DateFixerService
import no.nav.tsm.admin.datefixer.SykmeldingInputDateFixerConsumer

fun Application.configureAdminModule() {
    configureAdminDependencies()
    configureAdminRoutes()

    // Temporary until DateFixer has consumed all messages
    dependencies {
        provide(SykmeldingInputDateFixerConsumer::class)
        provide(DateFixerRepo::class)
        provide(DateFixerService::class)
    }

    startFixerConsumer()
}

private fun Application.startFixerConsumer() {
    val consumer: DateFixerService by dependencies

    monitor.subscribe(ApplicationStarted) { launch(Dispatchers.IO) { consumer.consumeWithRetry() } }
}
