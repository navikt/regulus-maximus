package no.nav.tsm.mottak.sykmelding.service

import no.nav.tsm.mottak.db.SykmeldingMapper
import no.nav.tsm.mottak.db.SykmeldingRepository
import no.nav.tsm.mottak.manuell.ManuellbehandlingService
import no.nav.tsm.mottak.sykmelding.exceptions.SykmeldingMergeValidationException
import no.nav.tsm.mottak.sykmelding.model.InvalidRule
import no.nav.tsm.mottak.sykmelding.model.OKRule
import no.nav.tsm.mottak.sykmelding.model.PendingRule
import no.nav.tsm.mottak.sykmelding.model.SykmeldingRecord
import no.nav.tsm.mottak.sykmelding.model.TilbakedatertMerknad
import no.nav.tsm.mottak.sykmelding.model.ValidationResult
import no.nav.tsm.mottak.sykmelding.model.ValidationType
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class SykmeldingService(
    private val sykmeldingRepository: SykmeldingRepository,
    private val kafkaTemplate: KafkaProducer<String, SykmeldingRecord>,
    private val manuellbehandlingService: ManuellbehandlingService,
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

        val newSykmeldingRecord = getManuellBehandlingRules(sykmelding)?.let {
            log.info("Sykmelding with id $sykmeldingId has been processed manually, overriding validation with $it")
            sykmelding.copy(validation = it)
        } ?: getOldValidation(sykmeldingId)?.let { oldValidation ->
            mergeSykmeldingWithOldValidation(sykmelding, oldValidation).also {
                log.info("Sykmelding with id $sykmeldingId has old validation $oldValidation, merging with new validation: ${sykmelding.validation}, merged ${it.validation}")
            }
        } ?: sykmelding

        if( newSykmeldingRecord.validation.rules.any { it is InvalidRule } && newSykmeldingRecord.validation.rules.any { it is OKRule }) {
            log.info("Sykmelding with id $sykmeldingId has invalid rules ${newSykmeldingRecord.validation}")
            throw SykmeldingMergeValidationException("Sykmelding with id $sykmeldingId has invalid rules, both ok and invalid")
        }

        insertAndSendSykmelding(newSykmeldingRecord)
    }

    private fun getOldValidation(sykmeldingId: String): ValidationResult? {
        return sykmeldingRepository.findBySykmeldingId(sykmeldingId)?.let { SykmeldingMapper.toSykmeldingRecord(it).validation }
    }

    private fun mergeSykmeldingWithOldValidation(sykmelding: SykmeldingRecord, oldValidation: ValidationResult) : SykmeldingRecord {
        val newSykmelding = sykmelding.sykmelding
        val metadata = sykmelding.metadata

        val mergedValidation = SykmeldingMapper.mergeValidations(
            old = oldValidation,
            new = sykmelding.validation
        )
        val times =  mergedValidation.rules.map { it.timestamp }
        if(times.size != times.distinct().size) {
            throw SykmeldingMergeValidationException("Sykmelding rulevalidations with same timestamps but different rules ${sykmelding.sykmelding.id}")
        }
        return SykmeldingRecord(metadata, newSykmelding , mergedValidation)
    }

    fun getManuellBehandlingRules(sykmelding: SykmeldingRecord): ValidationResult? {
        val manualDoneTimestamp = manuellbehandlingService.getManuellBehandlingTimestamp(sykmelding.sykmelding.id)
        when (manualDoneTimestamp) {
            null -> return null
            else -> {
                val pendingRule = PendingRule(
                    name = TilbakedatertMerknad.TILBAKEDATERING_UNDER_BEHANDLING.name,
                    timestamp = sykmelding.sykmelding.metadata.mottattDato,
                    description = "Tilbakedatert sykmelding til manuell behandling",
                    validationType = ValidationType.AUTOMATIC
                )

                val rules = sykmelding.validation.rules
                if (rules.size > 1) {
                    throw SykmeldingMergeValidationException("Sykmelding ${sykmelding.sykmelding.id}has more than one rule when it should only have one")
                }

                val newRule = if (rules.isEmpty()) {
                    okRule(pendingRule, manualDoneTimestamp)
                } else {
                    when (val rule = rules.single()) {
                        is InvalidRule -> rule.copy(timestamp = manualDoneTimestamp)
                        is OKRule -> rule.copy(timestamp = manualDoneTimestamp)
                        is PendingRule -> {
                            log.warn("Sykmelding ${sykmelding.sykmelding.id} has a pending rule that is not valid maybe $rule")
                            rule.copy(timestamp = manualDoneTimestamp)
                        }
                    }
                }

                return ValidationResult(
                    status = newRule.type,
                    timestamp = newRule.timestamp,
                    rules = listOf(pendingRule, newRule).sortedByDescending { it.timestamp }
                )
            }
        }
    }

    private fun okRule(pendingRule: PendingRule, manualDoneTimestamp: OffsetDateTime) = OKRule(
        timestamp = manualDoneTimestamp,
        name = pendingRule.name,
        description = pendingRule.description,
        validationType = ValidationType.MANUAL
    )

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
