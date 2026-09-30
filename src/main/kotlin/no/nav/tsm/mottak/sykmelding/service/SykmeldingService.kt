package no.nav.tsm.mottak.sykmelding.service

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.right
import arrow.fx.coroutines.parZip
import no.nav.tsm.core.Environment
import no.nav.tsm.core.logData
import no.nav.tsm.ktor.logger
import no.nav.tsm.ktor.teamLogger
import no.nav.tsm.mottak.db.SykmeldingRepository
import no.nav.tsm.mottak.db.mergeValidations
import no.nav.tsm.mottak.db.toSpecificSykmeldingRecord
import no.nav.tsm.mottak.pdl.PdlArrowed
import no.nav.tsm.mottak.sykmelder.Sykmelder
import no.nav.tsm.mottak.sykmelder.SykmelderService
import no.nav.tsm.mottak.sykmelding.exceptions.SykmeldingMergeValidationException
import no.nav.tsm.pdl.Person
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.sykmelding.input.core.model.Rule
import no.nav.tsm.sykmelding.input.core.model.Sykmelding
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.SykmeldingType
import no.nav.tsm.sykmelding.input.core.model.ValidationResult
import no.nav.tsm.sykmelding.input.core.model.metadata.MessageMetadata
import no.nav.tsm.sykmelding.input.core.model.metadata.PersonIdType
import org.apache.kafka.common.header.Headers
import tools.jackson.module.kotlin.jacksonMapperBuilder

class SykmeldingService(
    private val sykmeldingRepository: SykmeldingRepository,
    private val sykmeldingProducerService: SykmeldingProducerService,
    private val ruleService: RuleService,
    private val sykmelderService: SykmelderService,
    private val pdlClient: PdlArrowed,
    env: Environment,
) {

    companion object {
        private val log = logger()
        private val teamlog = teamLogger()
    }

    enum class CreateErrors {
        PersonNotInPdl,
        RuleError,
        UnknownResourceError,
    }

    enum class GetErrors {
        NotFound,
        UnknownError,
    }

    private val behandlingsdagerIds: List<String> = env.behandlingsdagerIds

    init {
        log.info("behandlerids size is ${behandlingsdagerIds.size}")
    }

    suspend fun updateSykmelding(
        sykmeldingId: String,
        sykmelding: SykmeldingRecord,
        headers: Headers,
    ) {
        val newSykmeldingRecord =
            when (behandlingsdagerIds.contains(sykmeldingId)) {
                true -> {
                    log.info("Prosesserer sykmelding med behandlingsdager $sykmeldingId")
                    sykmelding
                }
                false -> processSykmelding(sykmeldingId, sykmelding)
            }

        if (newSykmeldingRecord.sykmelding.type == SykmeldingType.DIGITAL) {
            val newSykmeldingRecordDigital = newSykmeldingRecord as SykmeldingRecord.Digital
            getSykmeldingVerifyResources(newSykmeldingRecordDigital) { sykmelder, previous, pasient
                ->
                ruleService
                    .verify(
                        sykmelding = newSykmeldingRecordDigital,
                        historiskeSykmeldinger = previous,
                        sykmelder = sykmelder,
                        sykmeldt = pasient,
                    )
                    .mapLeft { CreateErrors.RuleError }
                    .map { (result, _) -> result }
                    .bind()
            }.fold(
                {error: CreateErrors ->
                    log.error("Error occured on SykmeldingType.DIGITAL, id: ${newSykmeldingRecord.sykmelding.id} $error")
                },
                {result: RegulaResult ->
                    log.info("Got result id: ${newSykmeldingRecord.sykmelding.id} ${jacksonMapperBuilder().build().writeValueAsString(result)}")
                }
            )
            //TODO sammenligne resultat som syk-inn-api får
        }

        sykmeldingRepository.upsertSykmelding(newSykmeldingRecord)
        sykmeldingProducerService.sendToTsmSykmelding(newSykmeldingRecord, headers)
    }

    suspend fun deleteSykmelding(sykmeldingId: String, headers: Headers) {
        delete(sykmeldingId)
        sykmeldingProducerService.tombstoneTsmSykmelding(sykmeldingId, headers)
    }

    private suspend fun processSykmelding(
        sykmeldingId: String,
        sykmelding: SykmeldingRecord,
    ): SykmeldingRecord {

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
        return newSykmeldingRecord
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

    private fun mergeSykmeldingWithOld(
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

    suspend fun byIdent(ident: String): Either<GetErrors, List<SykmeldingRecord>> {
        return sykmeldingRepository.allSykmeldingerLastThreeYearsForIdent(ident).right()
    }

    private suspend fun <Result> getSykmeldingVerifyResources(
        sykmelding: SykmeldingRecord.Digital,
        block:
            suspend Raise<CreateErrors>.(
                sykmelder: Sykmelder, previous: List<SykmeldingRecord>, pasient: Person,
            ) -> Result,
    ): Either<CreateErrors, Result> = either {
        parZip(
            {
                sykmelderService
                    .byHpr(
                        sykmelding.sykmelding.behandler.ids
                            .find { it.type == PersonIdType.HPR }!!
                            .id
                    )
                    .mapLeft { CreateErrors.UnknownResourceError }
                    .bind()
            },
            {
                pdlClient
                    .getPerson(sykmelding.sykmelding.pasient.fnr)
                    .mapLeft {
                        when (it) {
                            PdlArrowed.PdlErrors.NotFound -> CreateErrors.PersonNotInPdl
                            PdlArrowed.PdlErrors.UnknownError -> CreateErrors.UnknownResourceError
                        }
                    }
                    .bind()
            },
            {
                byIdent(sykmelding.sykmelding.pasient.fnr)
                    .mapLeft { CreateErrors.UnknownResourceError }
                    .bind()
            },
        ) { sykmelder, pasient, previous ->
            block(sykmelder, previous, pasient)
        }
    }
}
