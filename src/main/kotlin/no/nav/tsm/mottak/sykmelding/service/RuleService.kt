package no.nav.tsm.mottak.sykmelding.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import java.time.LocalDateTime
import no.nav.tsm.ktor.logger
import no.nav.tsm.mottak.sykmelder.Sykmelder
import no.nav.tsm.mottak.sykmelding.mapPdlPersonToRegulaPasient
import no.nav.tsm.mottak.sykmelding.mapSykmelderToRegulaBehandler
import no.nav.tsm.mottak.sykmelding.mapUnruledSykInnSykmeldingToRegulaPayload
import no.nav.tsm.pdl.Person
import no.nav.tsm.regulus.regula.RegulaBehandler
import no.nav.tsm.regulus.regula.RegulaJuridiskVurdering
import no.nav.tsm.regulus.regula.RegulaPasient
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.regulus.regula.executor.ExecutionMode
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord

enum class RuleErrors {
    InvalidPatient
}

class RuleService {

    private val logger = logger()

    fun verify(
        sykmelding: SykmeldingRecord.Digital,
        historiskeSykmeldinger: List<SykmeldingRecord>,
        sykmelder: Sykmelder,
        sykmeldt: Person,
    ): Either<RuleErrors, Pair<RegulaResult, List<RegulaJuridiskVurdering>>> {
        val localdateNow = LocalDateTime.now()

        val regulaPasient = sykmeldt.mapPdlPersonToRegulaPasient()

        if (regulaPasient == null) {
            logger.error(
                "Unable to execute rules for pasient with missing or invalid ident or fødselsdato in PDL"
            )

            return RuleErrors.InvalidPatient.left()
        }

        val regulaBehandler = sykmelder.mapSykmelderToRegulaBehandler()

        return this.executeRegulaRules(
                behandletTidspunkt = localdateNow,
                sykmelding = sykmelding,
                historiskeSykmeldinger = historiskeSykmeldinger,
                behandler = regulaBehandler,
                pasient = regulaPasient,
            )
            .right()
    }

    private fun executeRegulaRules(
        behandletTidspunkt: LocalDateTime,
        sykmelding: SykmeldingRecord.Digital,
        historiskeSykmeldinger: List<SykmeldingRecord>,
        behandler: RegulaBehandler,
        pasient: RegulaPasient,
    ): Pair<RegulaResult, List<RegulaJuridiskVurdering>> {
        val regulaExecutionPayload =
            mapUnruledSykInnSykmeldingToRegulaPayload(
                behandletTidspunkt = behandletTidspunkt,
                sykmelding = sykmelding,
                otherSykmeldinger = historiskeSykmeldinger,
                behandler = behandler,
                pasient = pasient,
            )

        val result =
            no.nav.tsm.regulus.regula.executeRegulaRules(
                ruleExecutionPayload = regulaExecutionPayload,
                mode = ExecutionMode.NORMAL,
            )

        return result to result.juridisk
    }
}
