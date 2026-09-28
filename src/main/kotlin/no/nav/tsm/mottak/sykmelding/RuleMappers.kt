package no.nav.tsm.mottak.sykmelding

import no.nav.tsm.pdl.IdentGruppe
import no.nav.tsm.pdl.Person
import no.nav.tsm.regulus.regula.RegulaBehandler
import no.nav.tsm.regulus.regula.RegulaPasient
import no.nav.tsm.sykmelding.input.core.model.Sykmelder
import java.time.LocalDate

/*
fun Sykmelder.mapSykmelderToRegulaBehandler(legekontorOrgnummer: String): RegulaBehandler =
    when (this) {
        is Sykmelder.FinnesIkke -> RegulaBehandler.FinnesIkke

        is Sykmelder.MedSuspensjon ->
            RegulaBehandler.Finnes(
                suspendert = this.suspendert,
                godkjenninger = this.godkjenninger,
                legekontorOrgnr = legekontorOrgnummer,
                fnr = this.ident,
            )
    }

*/

fun Person.mapPdlPersonToRegulaPasient(): RegulaPasient? {
    val folkeregisterIdent =
        this.identer.firstOrNull { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT }
    if (folkeregisterIdent == null) return null
    if (this.foedselsdato == null) return null

    return RegulaPasient(
        ident = folkeregisterIdent.ident,
        fodselsdato = this.foedselsdato as LocalDate,
    )
}
