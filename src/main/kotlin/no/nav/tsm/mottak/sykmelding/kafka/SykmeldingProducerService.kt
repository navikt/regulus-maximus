package no.nav.tsm.mottak.sykmelding.kafka

import no.nav.tsm.core.logger
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import org.apache.kafka.common.header.Headers

class SykmeldingProducerService(private val sykmeldingProducer: SykmeldingProducer) {
    private val logger = logger()

    fun sendToTsmSykmelding(sykmelding: SykmeldingRecord, headers: Headers) {
        try {
            sykmeldingProducer.send(sykmelding, headers)
        } catch (exception: Exception) {
            logger.error("Failed to publish sykmelding to tsm.sykmelding", exception)
            throw exception
        }
    }

    fun tombstoneTsmSykmelding(sykmeldingId: String, headers: Headers) {
        try {
            sykmeldingProducer.tombstone(sykmeldingId, headers)
        } catch (exception: Exception) {
            logger.error("Failed to tombstone sykmelding to tsm.tsm-sykmelding", exception)
            throw exception
        }
    }
}
