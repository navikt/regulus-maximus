package no.nav.tsm.mottak.sykmelding

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.tsm.diagnoser.ICD10
import no.nav.tsm.diagnoser.ICPC2
import no.nav.tsm.diagnoser.ICPC2B
import no.nav.tsm.mottak.sykmelder.Sykmelder
import no.nav.tsm.pdl.IdentGruppe
import no.nav.tsm.pdl.Person
import no.nav.tsm.regulus.regula.RegulaAvsender
import no.nav.tsm.regulus.regula.RegulaBehandler
import no.nav.tsm.regulus.regula.RegulaMeta
import no.nav.tsm.regulus.regula.RegulaPasient
import no.nav.tsm.regulus.regula.RegulaPayload
import no.nav.tsm.regulus.regula.RegulaStatus
import no.nav.tsm.regulus.regula.payload.Aktivitet
import no.nav.tsm.regulus.regula.payload.AnnenFravarsArsak
import no.nav.tsm.regulus.regula.payload.Diagnose
import no.nav.tsm.regulus.regula.payload.RelevanteMerknader
import no.nav.tsm.regulus.regula.payload.TidligereSykmelding
import no.nav.tsm.regulus.regula.payload.TidligereSykmeldingAktivitet
import no.nav.tsm.regulus.regula.payload.TidligereSykmeldingMeta
import no.nav.tsm.sykmelding.input.core.model.DiagnoseInfo
import no.nav.tsm.sykmelding.input.core.model.DiagnoseSystem
import no.nav.tsm.sykmelding.input.core.model.RuleType
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord

fun Sykmelder.mapSykmelderToRegulaBehandler(): RegulaBehandler =
    when (this) {
        is Sykmelder.FinnesIkke -> RegulaBehandler.FinnesIkke

        is Sykmelder.MedSuspensjon ->
            RegulaBehandler.Finnes(
                suspendert = this.suspendert,
                godkjenninger = this.godkjenninger,
                legekontorOrgnr = null,
                fnr = this.ident,
            )
    }

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

fun mapUnruledSykInnSykmeldingToRegulaPayload(
    behandletTidspunkt: LocalDateTime,
    sykmelding: SykmeldingRecord.Digital,
    otherSykmeldinger: List<SykmeldingRecord>,
    behandler: RegulaBehandler,
    pasient: RegulaPasient,
): RegulaPayload {
    val avsender =
        if (behandler is RegulaBehandler.Finnes) RegulaAvsender.Finnes(fnr = behandler.fnr)
        else RegulaAvsender.IngenAvsender

    return RegulaPayload(
        meta = RegulaMeta.Meta(sendtTidspunkt = LocalDateTime.now()),
        pasient = pasient,
        behandler = behandler,
        avsender = avsender,
        hoveddiagnose = sykmelding.sykmelding.medisinskVurdering.hovedDiagnose?.toRegulaDiagnose(),
        bidiagnoser =
            sykmelding.sykmelding.medisinskVurdering.biDiagnoser?.map { it.toRegulaDiagnose() },
        aktivitet = sykmelding.sykmelding.aktivitet.map { it.toRegulaAktivitet() },
        annenFravarsArsak =
            sykmelding.sykmelding.medisinskVurdering.annenFravarsgrunn?.let {
                AnnenFravarsArsak(grunn = listOf(it.name), beskrivelse = null)
            },
        tidligereSykmeldinger = otherSykmeldinger.map { it.toTidligereSykmelding() },
        besvarteUtdypendeOpplysninger = emptyList(), // Regulus regula sjekker kun uke 39 spørsmål for versjon 2 av xml, ikke digital sykmelding
        kontaktPasientBegrunnelseIkkeKontakt = sykmelding.sykmelding.tilbakedatering?.begrunnelse,
        behandletTidspunkt = behandletTidspunkt,
    )
}

private fun DiagnoseSystem.toOID() =
    when (this) {
        DiagnoseSystem.ICPC2 -> ICPC2.OID
        DiagnoseSystem.ICD10 -> ICD10.OID
        DiagnoseSystem.ICPC2B -> ICPC2B.OID
        else ->
            error(
                "A DIGITAL to be ruled should never have any non-supported DiagnoseSystem: ${this}"
            )
    }

private fun DiagnoseInfo.toRegulaDiagnose(): Diagnose {
    return Diagnose(kode = kode, system = system.toOID())
}

private fun no.nav.tsm.sykmelding.input.core.model.Aktivitet.toRegulaAktivitet(): Aktivitet =
    when (this) {
        is no.nav.tsm.sykmelding.input.core.model.Aktivitet.IkkeMulig ->
            Aktivitet.IkkeMulig(fom = fom, tom = tom)
        is no.nav.tsm.sykmelding.input.core.model.Aktivitet.Gradert ->
            Aktivitet.Gradert(fom = fom, tom = tom, grad = grad)
        is no.nav.tsm.sykmelding.input.core.model.Aktivitet.Avventende ->
            Aktivitet.Avventende(
                fom = fom,
                tom = tom,
                avventendeInnspillTilArbeidsgiver = innspillTilArbeidsgiver,
            )

        is no.nav.tsm.sykmelding.input.core.model.Aktivitet.Behandlingsdager ->
            Aktivitet.Behandlingsdager(
                fom = fom,
                tom = tom,
                behandlingsdager = antallBehandlingsdager,
            )

        is no.nav.tsm.sykmelding.input.core.model.Aktivitet.Reisetilskudd ->
            Aktivitet.Reisetilskudd(fom = fom, tom = tom)
    }

private fun SykmeldingRecord.toTidligereSykmelding(): TidligereSykmelding {
    return TidligereSykmelding(
        sykmeldingId = sykmelding.id,
        hoveddiagnose = sykmelding.medisinskVurdering.hovedDiagnose?.toRegulaDiagnose(),
        aktivitet = sykmelding.aktivitet.map { it.toTidligereAktivitet() },
        meta =
            TidligereSykmeldingMeta(
                status =
                    when (validation.status) {
                        RuleType.OK -> RegulaStatus.OK
                        RuleType.PENDING -> RegulaStatus.MANUAL_PROCESSING
                        RuleType.INVALID -> RegulaStatus.INVALID
                    },
                userAction = "IKKE_RELEVANT",
                merknader =
                    if (validation.status == RuleType.PENDING) {
                        listOf(RelevanteMerknader.UNDER_BEHANDLING)
                    } else emptyList(),
            ),
    )
}

private fun no.nav.tsm.sykmelding.input.core.model.Aktivitet.toTidligereAktivitet():
    TidligereSykmeldingAktivitet =
    when (this) {
        is no.nav.tsm.sykmelding.input.core.model.Aktivitet.IkkeMulig ->
            TidligereSykmeldingAktivitet.IkkeMulig(fom = fom, tom = tom)
        is no.nav.tsm.sykmelding.input.core.model.Aktivitet.Avventende ->
            TidligereSykmeldingAktivitet.Avventende(fom = fom, tom = tom)
        is no.nav.tsm.sykmelding.input.core.model.Aktivitet.Behandlingsdager ->
            TidligereSykmeldingAktivitet.Behandlingsdager(fom = fom, tom = tom)
        is no.nav.tsm.sykmelding.input.core.model.Aktivitet.Gradert ->
            TidligereSykmeldingAktivitet.Gradert(fom = fom, tom = tom, grad = grad)
        is no.nav.tsm.sykmelding.input.core.model.Aktivitet.Reisetilskudd ->
            TidligereSykmeldingAktivitet.Reisetilskudd(fom = fom, tom = tom)
    }
