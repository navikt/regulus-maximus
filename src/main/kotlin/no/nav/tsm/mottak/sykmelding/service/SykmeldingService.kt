package no.nav.tsm.mottak.sykmelding.service

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tsm.mottak.db.SykmeldingMapper
import no.nav.tsm.mottak.db.SykmeldingRepository
import no.nav.tsm.mottak.manuell.ManuellbehandlingService
import no.nav.tsm.mottak.pdl.PdlClient
import no.nav.tsm.mottak.sykmelding.exceptions.SykmeldingMergeValidationException
import no.nav.tsm.mottak.sykmelding.kafka.PROCESSING_TARGET_HEADER
import no.nav.tsm.mottak.sykmelding.kafka.TSM_PROCESSING_TARGET
import no.nav.tsm.mottak.sykmelding.kafka.objectMapper
import no.nav.tsm.mottak.sykmelding.model.InvalidRule
import no.nav.tsm.mottak.sykmelding.model.OKRule
import no.nav.tsm.mottak.sykmelding.model.PendingRule
import no.nav.tsm.mottak.sykmelding.model.Reason
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
    private val pdlClient: PdlClient,
    private val manuellbehandlingService: ManuellbehandlingService,
    @Value("\${spring.kafka.topics.sykmeldinger-output}") private val tsmSykmeldingTopic: String,
) {

    companion object {
        private val log = LoggerFactory.getLogger(SykmeldingService::class.java)
    }

    @Transactional
    fun updateSykmelding(sykmeldingId: String, sykmelding: SykmeldingRecord?, processingTarget: String?) {
        if (sykmelding == null) {
            delete(sykmeldingId)
            tombStone(sykmeldingId)
            return
        }
//        val person = pdlClient.getPerson(sykmelding.sykmelding.pasient.fnr)
//        val aktorId = person.identer.first { it.gruppe == IDENT_GRUPPE.AKTORID && !it.historisk }.ident
//
//        val currentIdent = person.identer.first { !it.historisk && it.gruppe == IDENT_GRUPPE.FOLKEREGISTERIDENT }
//
//        if(currentIdent.ident != sykmelding.sykmelding.pasient.fnr) {
//            log.warn("Sykmelding with id $sykmeldingId has differnt aktive ident for aktorId $aktorId")
//        }

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

        insertAndSendSykmelding(newSykmeldingRecord, processingTarget)
    }

    private fun getOldValidation(sykmeldingId: String): ValidationResult? {
        return sykmeldingRepository.findBySykmeldingId(sykmeldingId)?.let {
            val validation = it.validation.value
            requireNotNull(validation)
            objectMapper.readValue(validation)
        }
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
                    reason = Reason(
                        sykmeldt ="Sykmeldingen blir manuelt behandlet fordi den er tilbakedatert",
                        sykmelder = "Sykmeldingen blir manuelt behandlet fordi den er tilbakedatert"),
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
        validationType = ValidationType.MANUAL
    )

    private fun insertAndSendSykmelding(sykmelding: SykmeldingRecord, processingTarget: String?) {
        sykmeldingRepository.upsertSykmelding(SykmeldingMapper.toSykmeldingDB(sykmelding))
        sendToTsmSykmelding(sykmelding, processingTarget)
    }


    private fun delete(sykmeldingId: String) {
        val deleted = sykmeldingRepository.deleteBySykmeldingId(sykmeldingId)
        log.info("Deleted $deleted sykmelding with id $sykmeldingId")
    }

    private fun sendToTsmSykmelding(sykmelding: SykmeldingRecord, processingTarget: String?) {
        try {
            val producerRecord = ProducerRecord(
                tsmSykmeldingTopic,
                sykmelding.sykmelding.id,
                SykmeldingRecord(
                    sykmelding = sykmelding.sykmelding,
                    metadata = sykmelding.metadata,
                    validation = sykmelding.validation
                )
            )
            if(processingTarget == TSM_PROCESSING_TARGET) {
                log.info("$PROCESSING_TARGET_HEADER is $processingTarget")
                producerRecord.headers().add(PROCESSING_TARGET_HEADER, TSM_PROCESSING_TARGET.toByteArray(Charsets.UTF_8))
            }
            kafkaTemplate.send(
                producerRecord
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
