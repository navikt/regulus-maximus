package no.nav.tsm.mottak.sykmelding.service

import no.nav.tsm.core.logData
import no.nav.tsm.core.logger
import no.nav.tsm.core.teamLogger
import no.nav.tsm.mottak.db.SykmeldingRepository
import no.nav.tsm.mottak.db.mergeValidations
import no.nav.tsm.mottak.db.toSpecificSykmeldingRecord
import no.nav.tsm.mottak.sykmelding.exceptions.SykmeldingMergeValidationException
import no.nav.tsm.mottak.sykmelding.kafka.SykmeldingProducerService
import no.nav.tsm.sykmelding.input.core.model.Rule
import no.nav.tsm.sykmelding.input.core.model.Sykmelding
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.ValidationResult
import no.nav.tsm.sykmelding.input.core.model.metadata.MessageMetadata
import org.apache.kafka.common.header.Headers

class SykmeldingService(
    private val sykmeldingRepository: SykmeldingRepository,
    private val sykmeldingProducerService: SykmeldingProducerService,
) {

    companion object {
        private val log = logger()
        private val teamlog = teamLogger()
    }

    suspend fun updateSykmelding(
        sykmeldingId: String,
        sykmelding: SykmeldingRecord?,
        headers: Headers,
    ) {
        if (sykmelding == null) {
            delete(sykmeldingId)
            sykmeldingProducerService.tombstoneTsmSykmelding(sykmeldingId, headers)
            return
        }

        val newSykmeldingRecord =
            sykmeldingRepository.findBySykmeldingId(sykmeldingId)?.let { oldSykmeldingRecord ->
                mergeSykmeldingWithOld(sykmelding, oldSykmeldingRecord).also {
                    log.info(
                        "Sykmelding with id $sykmeldingId has old validation ${oldSykmeldingRecord.validation}, merging with new validation: ${sykmelding.validation}, merged ${it.validation}"
                    )
                }
            } ?: sykmelding

        if (
            newSykmeldingRecord.validation.rules.any { it is Rule.Invalid } &&
                newSykmeldingRecord.validation.rules.any { it is Rule.OK }
        ) {
            log.info(
                "Sykmelding with id $sykmeldingId has invalid rules ${newSykmeldingRecord.validation}"
            )
            throw SykmeldingMergeValidationException(
                "Sykmelding with id $sykmeldingId has invalid rules, both ok and invalid"
            )
        }

        sykmeldingRepository.upsertSykmelding(newSykmeldingRecord)
        sykmeldingProducerService.sendToTsmSykmelding(newSykmeldingRecord, headers)
    }

    private fun mergeValidation(
        sykmeldingId: String,
        new: ValidationResult,
        old: ValidationResult,
    ): ValidationResult {
        if (new == old) {
            return new
        }

        if (old.timestamp >= new.timestamp) {
            log.info(
                "Sykmelding with id $sykmeldingId has newer validation in DB $old, merging with new validation: $new"
            )
            return old
        }

        val mergedValidation = mergeValidations(old = old, new = new)
        val times = mergedValidation.rules.map { it.timestamp }
        if (times.size != times.distinct().size) {
            throw SykmeldingMergeValidationException(
                "Sykmelding rulevalidations with same timestamps but different rules $sykmeldingId"
            )
        }
        return mergedValidation
    }

    fun mergeSykmeldingWithOld(
        sykmelding: SykmeldingRecord,
        oldSykmeldingRecord: SykmeldingRecord,
    ): SykmeldingRecord {
        val newSykmelding = sykmelding.sykmelding
        val metadata = sykmelding.metadata

        val mergedValidation =
            mergeValidation(
                sykmeldingId = sykmelding.sykmelding.id,
                new = sykmelding.validation,
                old = oldSykmeldingRecord.validation,
            )

        val times = mergedValidation.rules.map { it.timestamp }
        if (times.size != times.distinct().size) {
            throw SykmeldingMergeValidationException(
                "Sykmelding rulevalidations with same timestamps but different rules ${sykmelding.sykmelding.id}"
            )
        }

        checkSykmeldingData(sykmelding, oldSykmeldingRecord)

        return toSpecificSykmeldingRecord(
            sykmelding = newSykmelding,
            metadata = metadata,
            validation = mergedValidation,
        )
    }

    private fun checkSykmeldingData(
        sykmelding: SykmeldingRecord,
        oldSykmeldingRecord: SykmeldingRecord,
    ) {
        checkMetadata(
            sykmeldingId = sykmelding.sykmelding.id,
            newMetadata = sykmelding.metadata,
            oldMetadata = oldSykmeldingRecord.metadata,
        )
        checkSykmelding(
            sykmeldingId = sykmelding.sykmelding.id,
            newSykmelding = sykmelding.sykmelding,
            oldSykmelding = oldSykmeldingRecord.sykmelding,
        )
    }

    private fun checkSykmelding(
        sykmeldingId: String,
        newSykmelding: Sykmelding,
        oldSykmelding: Sykmelding,
    ) {
        if (newSykmelding == oldSykmelding) {
            return
        }

        teamlog.warn(
            "Sykmelding is not the same for ${newSykmelding.type}: $sykmeldingId. new: ${newSykmelding.logData()}, old: ${oldSykmelding.logData()}"
        )
    }

    private fun checkMetadata(
        sykmeldingId: String,
        newMetadata: MessageMetadata,
        oldMetadata: MessageMetadata,
    ) {
        if (newMetadata == oldMetadata) {
            return
        }
        teamlog.warn(
            "Sykmelding meta is not the same for ${newMetadata.type}: $sykmeldingId. new: ${newMetadata.logData()}, old: ${oldMetadata.logData()}"
        )
    }

    private suspend fun delete(sykmeldingId: String) {
        val deleted = sykmeldingRepository.deleteBySykmeldingId(sykmeldingId)
        log.info("Deleted $deleted sykmelding with id $sykmeldingId")
    }
}
