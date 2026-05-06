package no.nav.tsm.mottak.db

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tsm.mottak.sykmelding.exceptions.SykmeldingMergeValidationException
import no.nav.tsm.mottak.util.applog
import no.nav.tsm.sykmelding.input.core.model.*
import no.nav.tsm.sykmelding.input.core.model.metadata.MessageMetadata
import no.nav.tsm.sykmelding.input.core.model.metadata.MetadataType
import org.postgresql.util.PGobject
import java.time.LocalDate
import kotlin.reflect.KClass

class SykmeldingDBMappingException(message: String, ex: Exception) : Exception(message, ex)

class MessageMetadataDeserializer : CustomDeserializer<MessageMetadata>() {
    override fun getClass(type: String): KClass<out MessageMetadata> {
        return when (MetadataType.valueOf(type)) {
            MetadataType.ENKEL -> MessageMetadata.Xml.Emottak.Legacy::class
            MetadataType.EMOTTAK -> MessageMetadata.Xml.Emottak.EDI::class
            MetadataType.EGENMELDT -> MessageMetadata.Xml.Egenmeldt::class
            MetadataType.DIGITAL -> MessageMetadata.Digital::class
            MetadataType.UTENLANDSK_SYKMELDING -> MessageMetadata.Utenlandsk::class
            MetadataType.PAPIRSYKMELDING -> MessageMetadata.Papir::class
        }
    }
}
class MetadataModule : SimpleModule() {
    init {
        addDeserializer(MessageMetadata::class.java, MessageMetadataDeserializer())
    }
}


val sykmeldingObjectMapper =
    jacksonObjectMapper().apply {
        registerModule(SykmeldingModule())
        registerModule(MetadataModule())
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }

object SykmeldingMapper {

    private val logger = applog()

    fun toSykmeldingDB(
        sykmeldingMedBehandlingsutfall: SykmeldingRecord
    ): SykmeldingDB {
        try {
            return SykmeldingDB(
                sykmeldingId = sykmeldingMedBehandlingsutfall.sykmelding.id,
                pasientIdent = sykmeldingMedBehandlingsutfall.sykmelding.pasient.fnr,
                fom = sykmeldingMedBehandlingsutfall.sykmelding.aktivitet.earliestFom(),
                tom = sykmeldingMedBehandlingsutfall.sykmelding.aktivitet.latestTom(),
                generatedDate = sykmeldingMedBehandlingsutfall.sykmelding.metadata.genDate,
                sykmelding = sykmeldingMedBehandlingsutfall.sykmelding.toPGobject(),
                validation = sykmeldingMedBehandlingsutfall.validation.toPGobject(),
                metadata = sykmeldingMedBehandlingsutfall.metadata.toPGobject(),
            )
        } catch (ex: Exception) {
            throw SykmeldingDBMappingException("Failed to map sykmelding to SykmeldingDB: ${ex.message}", ex)
        }
    }

    fun toSykmeldingRecord(sykmeldingDB: SykmeldingDB): SykmeldingRecord {
        val sykmeldingJson = sykmeldingDB.sykmelding.value
        val metadataJson = sykmeldingDB.metadata.value
        val validationJson = sykmeldingDB.validation.value

        requireNotNull(sykmeldingJson)
        requireNotNull(metadataJson)
        requireNotNull(validationJson)

        val sykmelding: Sykmelding = sykmeldingObjectMapper.readValue(sykmeldingJson)
        val metadata: MessageMetadata = sykmeldingObjectMapper.readValue(metadataJson)
        val validation: ValidationResult = sykmeldingObjectMapper.readValue(validationJson)

        return toSpecificSykmeldingRecord(sykmelding, metadata, validation)
    }


    fun mergeValidations(old: ValidationResult, new: ValidationResult): ValidationResult {
        if (old == new) {
            return new
        }

        val rule = when (old.status) {
            RuleType.PENDING -> {
                if (new.rules.isEmpty()) {
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
                if (new.status != old.status || new.timestamp != old.timestamp || !old.rules.containsAll(new.rules)) {
                    if (new.status == RuleType.OK && old.status == RuleType.OK && old.rules.containsAll(new.rules)) {
                        logger.warn("Ignoring validation result with status OK with different timestamps")
                    } else {
                        throw SykmeldingMergeValidationException("Cannot merge from ${old.status} to ${new.status}")
                    }
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
            RuleType.OK -> Rule.OK(
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

private inline fun <reified T: MessageMetadata> MessageMetadata.requireType(): T = when (this) {
    is T -> this
    else -> throw IllegalStateException("Invalid metadata type: ${type}", )
}
fun toSpecificSykmeldingRecord(
    sykmelding: Sykmelding,
    metadata: MessageMetadata,
    validation: ValidationResult
): SykmeldingRecord {
    return when (sykmelding) {
        is Sykmelding.Digital -> SykmeldingRecord.Digital(
            metadata = metadata.requireType(),
            sykmelding = sykmelding,
            validation = validation,
        )

        is Sykmelding.Papir -> SykmeldingRecord.Papir(
            metadata = metadata.requireType(),
            sykmelding = sykmelding,
            validation = validation,
        )

        is Sykmelding.Xml -> SykmeldingRecord.Xml(
            metadata = metadata.requireType(),
            sykmelding = sykmelding,
            validation = validation,
        )

        is Sykmelding.Utenlandsk -> SykmeldingRecord.Utenlandsk(
            metadata = metadata.requireType(),
            sykmelding = sykmelding,
            validation = validation,
        )
    }
}

fun List<Aktivitet>.earliestFom(): LocalDate = minBy { it.fom }.fom

fun List<Aktivitet>.latestTom(): LocalDate = maxBy { it.tom }.tom

fun Any.toPGobject(): PGobject {
    return PGobject().also {
        it.value = sykmeldingObjectMapper.writeValueAsString(this)
        it.type = "json"
    }
}
