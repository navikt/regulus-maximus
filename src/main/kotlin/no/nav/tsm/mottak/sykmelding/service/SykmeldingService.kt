package no.nav.tsm.mottak.sykmelding.service

import no.nav.tsm.mottak.db.SykmeldingMapper
import no.nav.tsm.mottak.db.SykmeldingRepository
import no.nav.tsm.mottak.pdl.IDENT_GRUPPE
import no.nav.tsm.mottak.pdl.PdlClient
import no.nav.tsm.mottak.sykmelding.model.SykmeldingRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SykmeldingService(
    private val sykmeldingRepository: SykmeldingRepository,
    private val kafkaTemplate: KafkaProducer<String, SykmeldingRecord>,
    private val pdlClient: PdlClient,
    @Value("\${spring.kafka.topics.sykmeldinger-output}") private val tsmSykmeldingTopic: String,
) {

    companion object {
        private val log = LoggerFactory.getLogger(SykmeldingService::class.java)
    }

    @Transactional
    fun updateSykmelding(sykmeldingId: String, sykmelding: SykmeldingRecord?) {
        if (sykmelding == null) {
            delete(sykmeldingId)
            tombStone(sykmeldingId)
            return
        }
        val person = pdlClient.getPerson(sykmelding.sykmelding.pasient.fnr)
        if(person == null) {
            log.error("Person not found for fnr ${sykmelding.sykmelding.pasient.fnr}")
            throw RuntimeException("Person not found for sykmelding with id $sykmeldingId")
        }
        val aktorId = person.identer.first { it.gruppe == IDENT_GRUPPE.AKTORID && !it.historisk }.ident
        log.info("Received sykmelding with id $sykmeldingId, aktorId: $aktorId")

        val currentIdent = person.identer.first { !it.historisk && it.ident == sykmelding.sykmelding.pasient.fnr }

        if(currentIdent.ident != sykmelding.sykmelding.pasient.fnr) {
            log.warn("Sykmelding with id $sykmeldingId has differnt aktive ident for aktorId $aktorId")
        }

        val previousSykmelding =
            sykmeldingRepository.findBySykmeldingId(sykmeldingId)?.let { SykmeldingMapper.toSykmeldingRecord(it) }
        when(previousSykmelding) {
            null -> insertAndSendSykmelding(sykmelding)
            else -> mergeAndSendSykmelding(sykmelding, previousSykmelding)
        }
    }

    private fun mergeAndSendSykmelding(sykmelding: SykmeldingRecord, previousSykmelding: SykmeldingRecord) {
        val newSykmelding = sykmelding.sykmelding
        val metadata = sykmelding.metadata

        val mergedValidation = SykmeldingMapper.mergeValidations(
            old = previousSykmelding.validation,
            new = sykmelding.validation
        )
        val mergedSykmelding = SykmeldingRecord(metadata, newSykmelding , mergedValidation)
        sykmeldingRepository.upsertSykmelding(SykmeldingMapper.toSykmeldingDB(mergedSykmelding))
        log.info("Merging sykmelding with id ${sykmelding.sykmelding.id}, old validation: ${previousSykmelding.validation}, new validation: ${sykmelding.validation} -> merged validation: $mergedValidation")
        sendToTsmSykmelding(mergedSykmelding)
    }

    private fun insertAndSendSykmelding(sykmelding: SykmeldingRecord) {
        sykmeldingRepository.upsertSykmelding(SykmeldingMapper.toSykmeldingDB(sykmelding))
        sendToTsmSykmelding(sykmelding)
    }

    private fun delete(sykmeldingId: String) {
        val deleted = sykmeldingRepository.deleteBySykmeldingId(sykmeldingId)
        log.info("Deleted $deleted sykmelding with id $sykmeldingId")
    }

    private fun sendToTsmSykmelding(sykmelding: SykmeldingRecord) {
        try {
            kafkaTemplate.send(
                ProducerRecord(
                    tsmSykmeldingTopic,
                    sykmelding.sykmelding.id,
                    SykmeldingRecord(
                        sykmelding = sykmelding.sykmelding,
                        metadata = sykmelding.metadata,
                        validation = sykmelding.validation
                    )
                )
            ).get()
        } catch (exception: Exception) {
            log.error("Failed to publish sykmelding to tsm.sykmelding", exception)
            throw exception
        }
    }

    private fun tombStone(sykmeldingId: String) {
        try {
            kafkaTemplate.send(
                ProducerRecord(
                    tsmSykmeldingTopic,
                    sykmeldingId,
                    null
                )
            ).get()
        } catch (exception: Exception) {
            log.error("Failed to tombstone sykmelding to tsm.tsm-sykmelding", exception)
            throw exception
        }
    }
}
