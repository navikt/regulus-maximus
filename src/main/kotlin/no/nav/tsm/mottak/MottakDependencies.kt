package no.nav.tsm.mottak

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import no.nav.tsm.core.dynamicDependencies
import no.nav.tsm.mottak.pdl.PdlCloudClient
import no.nav.tsm.mottak.pdl.PdlLocalClient
import no.nav.tsm.mottak.sykmelding.kafka.SykmeldingInputConsumer
import no.nav.tsm.mottak.sykmelding.kafka.SykmeldingInputConsumerService
import no.nav.tsm.mottak.sykmelding.kafka.SykmeldingProducer
import no.nav.tsm.mottak.sykmelding.kafka.SykmeldingProducerService
import no.nav.tsm.mottak.sykmelding.service.SykmeldingService

fun Application.configureMottakDependencies() {
    dynamicDependencies {
        local { provide(PdlLocalClient::class) }
        cloud { provide(PdlCloudClient::class) }
    }

    dependencies {
        provide(SykmeldingService::class)
        provide(SykmeldingInputConsumer::class)
        provide(SykmeldingInputConsumerService::class)
        provide(SykmeldingProducer::class)
        provide(SykmeldingProducerService::class)
    }
}
