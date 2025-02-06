package no.nav.tsm.mottak.db

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tsm.mottak.sykmelding.exceptions.SykmeldingMergeValidationException
import no.nav.tsm.mottak.sykmelding.model.SykmeldingRecord
import no.nav.tsm.mottak.sykmelding.model.OKRule
import no.nav.tsm.mottak.sykmelding.model.RuleType
import no.nav.tsm.mottak.sykmelding.model.ValidationResult
import no.nav.tsm.mottak.sykmelding.model.ValidationType
import no.nav.tsm.mottak.sykmelding.kafka.objectMapper
import org.postgresql.util.PGobject

object SykmeldingMapper {

    fun toSykmeldingDB(
        sykmeldingMedBehandlingsutfall: SykmeldingRecord
    ): SykmeldingDB {
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
        new: ValidationResult
    ): ValidationResult {
        val rule = old.rules.maxBy { it.timestamp }
        val newRule = when (new.status) {
            RuleType.OK -> OKRule(
                name = rule.name,
                description = rule.description,
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
