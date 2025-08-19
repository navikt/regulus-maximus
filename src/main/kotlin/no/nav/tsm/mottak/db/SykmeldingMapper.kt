package no.nav.tsm.mottak.db

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tsm.mottak.sykmelding.exceptions.SykmeldingMergeValidationException
import no.nav.tsm.mottak.sykmelding.kafka.objectMapper
import no.nav.tsm.mottak.util.applog
import no.nav.tsm.sykmelding.input.core.model.OKRule
import no.nav.tsm.sykmelding.input.core.model.RuleType
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.ValidationResult
import no.nav.tsm.sykmelding.input.core.model.ValidationType
import org.postgresql.util.PGobject
import org.slf4j.LoggerFactory

class SykmeldingDBMappingException(message: String, ex: Exception) : Exception(message, ex)

object SykmeldingMapper {

    private val logger = applog()

    fun toSykmeldingDB(
        sykmeldingMedBehandlingsutfall: SykmeldingRecord
    ): SykmeldingDB {
        try {
            return SykmeldingDB(
                sykmeldingId = sykmeldingMedBehandlingsutfall.sykmelding.id,
                pasientIdent = sykmeldingMedBehandlingsutfall.sykmelding.pasient.fnr,
                fom = sykmeldingMedBehandlingsutfall.sykmelding.aktivitet.first().fom,
                tom = sykmeldingMedBehandlingsutfall.sykmelding.aktivitet.last().tom,
                generatedDate = sykmeldingMedBehandlingsutfall.sykmelding.metadata.genDate,
                sykmelding =  sykmeldingMedBehandlingsutfall.sykmelding.toPGobject(),
                validation = sykmeldingMedBehandlingsutfall.validation.toPGobject(),
                metadata = sykmeldingMedBehandlingsutfall.metadata.toPGobject(),
            )
        } catch (ex: Exception) {
            throw SykmeldingDBMappingException("Failed to map sykmelding to SykmeldingDB: ${ex.message}", ex)
        }
    }

    fun toSykmeldingRecord(sykmeldingDB: SykmeldingDB): SykmeldingRecord {
        val sykmelding = sykmeldingDB.sykmelding.value
        val metadata = sykmeldingDB.metadata.value
        val validation = sykmeldingDB.validation.value
        requireNotNull(sykmelding)
        requireNotNull(metadata)
        requireNotNull(validation)
        return SykmeldingRecord(
            sykmelding = objectMapper.readValue(sykmelding),
            metadata = objectMapper.readValue(metadata),
            validation = objectMapper.readValue(validation),
        )
    }

    fun mergeValidations(old: ValidationResult, new: ValidationResult): ValidationResult {
        if(old == new) {
            return new
        }

        val rule = when(old.status) {
            RuleType.PENDING -> {
                if(new.rules.isEmpty()) {
                   return mergePendingWithEmpty(old, new)
                }
                val allRules = old.rules + new.rules

                val latestRule = allRules.maxBy { it.timestamp }

                return ValidationResult(
                    status = latestRule.type,
                    timestamp = latestRule.timestamp,
                    rules = allRules.sortedByDescending { it.timestamp }
                )
            }
            else -> {
                if(new.status != old.status || new.timestamp != old.timestamp || !old.rules.containsAll(new.rules)) {
                    throw SykmeldingMergeValidationException("Cannot merge from ${old.status} to ${new.status}")
                }
                old
            }
        }
        return rule
    }

    private fun mergePendingWithEmpty(
        old: ValidationResult,
        new: ValidationResult,
    ): ValidationResult {
        val rule = old.rules.maxBy { it.timestamp }
        val newRule = when (new.status) {
            RuleType.OK -> OKRule(
                name = rule.name,
                timestamp = new.timestamp,
                validationType = ValidationType.MANUAL
            )
            else -> throw SykmeldingMergeValidationException("Cannot merge from ${old.status} to ${new.status}")
        }
        return ValidationResult(
            status = new.status,
            timestamp = new.timestamp,
            rules = (old.rules + newRule).sortedByDescending { it.timestamp }
        )
    }
}

fun Any.toPGobject() : PGobject {
    return PGobject().also {
        it.value = objectMapper.writeValueAsString(this)
        it.type = "json"
    }
}
