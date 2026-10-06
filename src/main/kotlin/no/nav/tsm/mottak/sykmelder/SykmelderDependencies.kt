package no.nav.tsm.mottak.sykmelder

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import no.nav.tsm.ktor.di.dynamicDependencies
import no.nav.tsm.mottak.sykmelder.tsmBehandler.TsmBehandlerCloudClient
import no.nav.tsm.mottak.sykmelder.tsmBehandler.TsmBehandlerLocalClient

fun Application.configureSykmelderDependencies() {
    dynamicDependencies {
        local { provide(TsmBehandlerLocalClient::class) }
        cloud { provide(TsmBehandlerCloudClient::class) }
    }

    dependencies { provide(SykmelderService::class) }
}
