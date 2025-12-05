package no.nav.tsm.mottak.sykmelding.service

import no.nav.tsm.mottak.db.SykmeldingMapper
import no.nav.tsm.mottak.db.SykmeldingRepository
import no.nav.tsm.mottak.sykmelding.exceptions.SykmeldingMergeValidationException
import no.nav.tsm.mottak.util.applog
import no.nav.tsm.mottak.util.logData
import no.nav.tsm.mottak.util.teamLogger
import no.nav.tsm.sykmelding.input.core.model.InvalidRule
import no.nav.tsm.sykmelding.input.core.model.OKRule
import no.nav.tsm.sykmelding.input.core.model.Sykmelding
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.ValidationResult
import no.nav.tsm.sykmelding.input.core.model.metadata.MessageMetadata
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.Headers
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SykmeldingService(
    private val sykmeldingRepository: SykmeldingRepository,
    private val kafkaTemplate: KafkaProducer<String, SykmeldingRecord>,
    @Value("\${spring.kafka.topics.sykmeldinger-output}") private val tsmSykmeldingTopic: String,
) {

    companion object {
        private val log = applog()
        private val teamlog = teamLogger()
    }

    @Transactional
    fun updateSykmelding(sykmeldingId: String, sykmelding: SykmeldingRecord?, headers: Headers) {
        if (sykmelding == null) {
            delete(sykmeldingId)
            tombstone(sykmeldingId, headers)
            return
        }

        val newSykmeldingRecord = getOldSykmeldingRecord(sykmeldingId)?.let { oldSykmeldingRecord ->
            mergeSykmeldingWithOld(sykmelding, oldSykmeldingRecord).also {
                log.info("Sykmelding with id $sykmeldingId has old validation ${oldSykmeldingRecord.validation}, merging with new validation: ${sykmelding.validation}, merged ${it.validation}")
            }
        } ?: sykmelding

        if( newSykmeldingRecord.validation.rules.any { it is InvalidRule } && newSykmeldingRecord.validation.rules.any { it is OKRule }) {
            log.info("Sykmelding with id $sykmeldingId has invalid rules ${newSykmeldingRecord.validation}")

            throw SykmeldingMergeValidationException("Sykmelding with id $sykmeldingId has invalid rules, both ok and invalid")
        }

        insertOrUpdateSykmelding(newSykmeldingRecord)
        sendToTsmSykmelding(newSykmeldingRecord, headers)
    }

    private fun getOldSykmeldingRecord(sykmeldingId: String): SykmeldingRecord? {
        return sykmeldingRepository.findBySykmeldingId(sykmeldingId)?.let {
            SykmeldingMapper.toSykmeldingRecord(it)
        }
    }

    fun mergeValidation(sykmeldingId: String, new: ValidationResult, old: ValidationResult) : ValidationResult {
        if(new == old) {
            return new
        }

        if(old.timestamp >= new.timestamp) {
            log.info("Sykmelding with id $sykmeldingId has newer validation in DB $old, merging with new validation: $new")
            return old
        }

        val mergedValidation = SykmeldingMapper.mergeValidations(
            old = old,
            new = new
        )
        val times =  mergedValidation.rules.map { it.timestamp }
        if(times.size != times.distinct().size) {
            throw SykmeldingMergeValidationException("Sykmelding rulevalidations with same timestamps but different rules $sykmeldingId")
        }
        return mergedValidation
    }

    fun mergeSykmeldingWithOld(sykmelding: SykmeldingRecord, oldSykmeldingRecord: SykmeldingRecord) : SykmeldingRecord {
        val newSykmelding = sykmelding.sykmelding
        val metadata = sykmelding.metadata

        val mergedValidation = mergeValidation(
            sykmeldingId = sykmelding.sykmelding.id,
            new = sykmelding.validation,
            old = oldSykmeldingRecord.validation
        )

        val times =  mergedValidation.rules.map { it.timestamp }
        if(times.size != times.distinct().size) {
            throw SykmeldingMergeValidationException("Sykmelding rulevalidations with same timestamps but different rules ${sykmelding.sykmelding.id}")
        }

        checkSykmeldingData(sykmelding, oldSykmeldingRecord)

        return SykmeldingRecord(metadata, newSykmelding , mergedValidation)
    }

    private fun checkSykmeldingData(
        sykmelding: SykmeldingRecord,
        oldSykmeldingRecord: SykmeldingRecord
    ) {
        checkMetadata(
            sykmeldingId = sykmelding.sykmelding.id,
            newMetadata = sykmelding.metadata,
            oldMetadata = oldSykmeldingRecord.metadata
        )
        checkSykmelding(
            sykmeldingId = sykmelding.sykmelding.id,
            newSykmelding = sykmelding.sykmelding,
            oldSykmelding = oldSykmeldingRecord.sykmelding
        )
    }

    private fun checkSykmelding(
        sykmeldingId: String,
        newSykmelding: Sykmelding,
        oldSykmelding: Sykmelding
    ) {
        if(newSykmelding == oldSykmelding) {
            return
        }

        teamlog.warn("Sykmelding is not the same for ${newSykmelding.type}: $sykmeldingId. new: ${newSykmelding.logData()}, old: ${oldSykmelding.logData()}")
    }

    private fun checkMetadata(
        sykmeldingId: String,
        newMetadata: MessageMetadata,
        oldMetadata: MessageMetadata
    ) {
        if(newMetadata == oldMetadata) {
            return
        }
        teamlog.warn("Sykmelding meta is not the same for ${newMetadata.type}: $sykmeldingId. new: ${newMetadata.logData()}, old: ${oldMetadata.logData()}")
    }

    private fun insertOrUpdateSykmelding(sykmelding: SykmeldingRecord) {
        sykmeldingRepository.upsertSykmelding(SykmeldingMapper.toSykmeldingDB(sykmelding))
    }


    private fun delete(sykmeldingId: String) {
        val deleted = sykmeldingRepository.deleteBySykmeldingId(sykmeldingId)
        log.info("Deleted $deleted sykmelding with id $sykmeldingId")
    }

    private fun sendToTsmSykmelding(sykmelding: SykmeldingRecord, headers: Headers) {
        try {
            val producerRecord = ProducerRecord(
                tsmSykmeldingTopic,
                null,
                sykmelding.sykmelding.id,
                SykmeldingRecord(
                    sykmelding = sykmelding.sykmelding,
                    metadata = sykmelding.metadata,
                    validation = sykmelding.validation
                ),
                headers,
            )
            kafkaTemplate.send(
                producerRecord
            ).get()
        } catch (exception: Exception) {
            log.error("Failed to publish sykmelding to tsm.sykmelding", exception)
            throw exception
        }
    }

    private fun tombstone(sykmeldingId: String, headers: Headers) {
        try {
            kafkaTemplate.send(
                ProducerRecord(
                    tsmSykmeldingTopic,
                    null,
                    sykmeldingId,
                    null,
                    headers,
                )
            ).get()
        } catch (exception: Exception) {
            log.error("Failed to tombstone sykmelding to tsm.tsm-sykmelding", exception)
            throw exception
        }
    }
}
