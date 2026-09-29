package no.nav.tsm.mottak.sykmelding

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
import no.nav.tsm.sykmelding.input.core.model.RuleType
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import java.time.LocalDate
import java.time.LocalDateTime

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
        bidiagnoser = sykmelding.sykmelding.medisinskVurdering.biDiagnoser?.map { it.toRegulaDiagnose() },
        aktivitet = sykmelding.sykmelding.aktivitet.map { it.toRegulaAktivitet() },
        annenFravarsArsak =
            sykmelding.sykmelding.medisinskVurdering.annenFravarsgrunn?.let {
                AnnenFravarsArsak(grunn = listOf(it.name), beskrivelse = null)
            },
        tidligereSykmeldinger = otherSykmeldinger.map { it.toTidligereSykmelding() },
        besvarteUtdypendeOpplysninger =
            sykmelding.sykmelding.utdypendeSporsmal?.toRegulaBesvartUtdypende(),
        kontaktPasientBegrunnelseIkkeKontakt = sykmelding.values.tilbakedatering?.begrunnelse,
        behandletTidspunkt = behandletTidspunkt,
    )
}

private fun SykInnDiagnoseSystem.toOID() =
    when (this) {
        SykInnDiagnoseSystem.ICPC2 -> ICPC2.OID
        SykInnDiagnoseSystem.ICD10 -> ICD10.OID
        SykInnDiagnoseSystem.ICPC2B -> ICPC2B.OID
    }

private fun SykInnDiagnoseInfo.toRegulaDiagnose(): Diagnose {
    return Diagnose(
        kode = code,
        system =
            when (this) {
                is SykInnDiagnoseInfo.Valid -> system.toOID()
                is SykInnDiagnoseInfo.Invalid ->
                    try {
                        SykInnDiagnoseSystem.valueOf(system).toOID()
                    } catch (_: Exception) {
                        error(
                            "A DIGITAL to be ruled should never have any non-supported DiagnoseSystem: ${this.system}"
                        )
                    }
            },
    )
}

private fun SykInnAktivitet.toRegulaAktivitet(): Aktivitet =
    when (this) {
        is SykInnAktivitet.IkkeMulig -> Aktivitet.IkkeMulig(fom = fom, tom = tom)
        is SykInnAktivitet.Gradert -> Aktivitet.Gradert(fom = fom, tom = tom, grad = grad)
        is SykInnAktivitet.Avventende ->
            Aktivitet.Avventende(
                fom = fom,
                tom = tom,
                avventendeInnspillTilArbeidsgiver = innspillTilArbeidsgiver,
            )

        is SykInnAktivitet.Behandlingsdager ->
            Aktivitet.Behandlingsdager(
                fom = fom,
                tom = tom,
                behandlingsdager = antallBehandlingsdager,
            )

        is SykInnAktivitet.Reisetilskudd -> Aktivitet.Reisetilskudd(fom = fom, tom = tom)
    }

private fun VerifiedSykInnSykmelding.toTidligereSykmelding(): TidligereSykmelding {
    return TidligereSykmelding(
        sykmeldingId = sykmeldingId.toString(),
        hoveddiagnose = values.hoveddiagnose?.toRegulaDiagnose(),
        aktivitet = values.aktivitet.map { it.toTidligereAktivitet() },
        meta =
            TidligereSykmeldingMeta(
                status =
                    when (result) {
                        is SykInnSykmeldingRuleResult.OK -> RegulaStatus.OK
                        is SykInnSykmeldingRuleResult.Outcome ->
                            when (result.type) {
                                RuleType.OK -> RegulaStatus.OK
                                RuleType.PENDING -> RegulaStatus.MANUAL_PROCESSING
                                RuleType.INVALID -> RegulaStatus.INVALID
                            }
                    },
                userAction = "IKKE_RELEVANT",
                merknader =
                    if (
                        result is SykInnSykmeldingRuleResult.Outcome &&
                        result.type == RuleType.PENDING
                    ) {
                        listOf(RelevanteMerknader.UNDER_BEHANDLING)
                    } else emptyList(),
            ),
    )
}

private fun SykInnAktivitet.toTidligereAktivitet(): TidligereSykmeldingAktivitet =
    when (this) {
        is SykInnAktivitet.Avventende ->
            TidligereSykmeldingAktivitet.Avventende(fom = fom, tom = tom)

        is SykInnAktivitet.Behandlingsdager ->
            TidligereSykmeldingAktivitet.Behandlingsdager(fom = fom, tom = tom)

        is SykInnAktivitet.Gradert ->
            TidligereSykmeldingAktivitet.Gradert(fom = fom, tom = tom, grad = grad)

        is SykInnAktivitet.IkkeMulig -> TidligereSykmeldingAktivitet.IkkeMulig(fom = fom, tom = tom)
        is SykInnAktivitet.Reisetilskudd ->
            TidligereSykmeldingAktivitet.Reisetilskudd(fom = fom, tom = tom)
    }

