package no.nav.tsm.mottak.sykmelding.service

import arrow.core.Either
import arrow.core.left
import no.nav.tsm.ktor.logger
import no.nav.tsm.mottak.sykmelding.mapPdlPersonToRegulaPasient
import no.nav.tsm.pdl.Person
import no.nav.tsm.regulus.regula.RegulaJuridiskVurdering
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.sykmelding.input.core.model.Sykmelder
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import java.time.LocalDateTime

enum class RuleErrors {
    InvalidPatient
}

class RuleService {

    private val logger = logger()

    fun verify(
        sykmelding: SykmeldingRecord.Digital,
        historiskeSykmeldinger: List<SykmeldingRecord>,
        sykmelder: Sykmelder,
        sykmeldt: Person
    ): Either<RuleErrors, Pair<RegulaResult, List<RegulaJuridiskVurdering>>> {
        val now = LocalDateTime.now()

        val regulaPasient = sykmeldt.mapPdlPersonToRegulaPasient()

        if (regulaPasient == null) {
            logger.error(
                "Unable to execute rules for pasient with missing or invalid ident or fødselsdato in PDL"
            )

           return RuleErrors.InvalidPatient.left()
        }

        val regulaBehandler = sykmelder.



    }



    fun executeRegulaRules(){

    }

}