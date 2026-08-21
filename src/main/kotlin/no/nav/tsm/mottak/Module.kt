package no.nav.tsm.mottak

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import no.nav.tsm.core.Environment
import no.nav.tsm.ktor.kafka.consumer.KafkaConsumer
import no.nav.tsm.mottak.sykmelding.service.SykmeldingService
import no.nav.tsm.sykmelding.input.core.model.SykmeldingModule
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord

fun Application.configureMottakModule() {
    configureMottakDependencies()
    configureConsumer()
}

fun Application.configureConsumer() {
    val env: Environment by dependencies
    val service: SykmeldingService by dependencies

    install(KafkaConsumer) {
        clientId = env.runtime.name
        groupId = "regulus-maximus-consumer"
        consume<SykmeldingRecord>(
            name = "tsm.sykmeldinger-input",
            onRecord = { record, meta -> service.updateSykmelding(meta.key, record, meta.headers) },
            onTombstone = { service.deleteSykmelding(it.key, it.headers) },
        )
        jacksonModule(SykmeldingModule())
    }
}
