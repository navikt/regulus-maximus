package no.nav.tsm.mottak.sykmelding.service

import no.nav.tsm.ktor.kafka.producer.KafkaRecordProducer
import no.nav.tsm.ktor.logger
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import org.apache.kafka.common.header.Headers

class SykmeldingProducerService(
    private val sykmeldingProducer: KafkaRecordProducer<SykmeldingRecord>
) {
    private val logger = logger()

    fun sendToTsmSykmelding(sykmelding: SykmeldingRecord, headers: Headers) {
        try {
            sykmeldingProducer.send(sykmelding.sykmelding.id, sykmelding, headers.associate { it.key() to it.value().contentToString() })
        } catch (exception: Exception) {
            logger.error("Failed to publish sykmelding to tsm.sykmelding", exception)
            throw exception
        }
    }

    fun tombstoneTsmSykmelding(sykmeldingId: String, headers: Headers) {
        try {
            sykmeldingProducer.tombstone(sykmeldingId, headers.associate { it.key() to it.value().contentToString() })
        } catch (exception: Exception) {
            logger.error("Failed to tombstone sykmelding to tsm.tsm-sykmelding", exception)
            throw exception
        }
    }
}
