package no.nav.tsm.mottak

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import no.nav.tsm.ktor.clients.pdl.PdlPlugin
import no.nav.tsm.ktor.kafka.producer.KafkaRecordProducer
import no.nav.tsm.ktor.kafka.producer.createProducer
import no.nav.tsm.mottak.db.SykmeldingRepository
import no.nav.tsm.mottak.sykmelding.service.SykmeldingProducerService
import no.nav.tsm.mottak.sykmelding.service.SykmeldingService
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord

fun Application.configureMottakDependencies() {
    install(PdlPlugin)

    dependencies {
        provide(SykmeldingService::class)
        provide(SykmeldingRepository::class)
        provide<KafkaRecordProducer<SykmeldingRecord>> {
            this@configureMottakDependencies.createProducer(topic = "tsm.sykmeldinger")
        }
        provide(SykmeldingProducerService::class)
    }
}
