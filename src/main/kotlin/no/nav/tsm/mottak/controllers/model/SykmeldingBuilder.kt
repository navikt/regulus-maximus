package no.nav.tsm.mottak.controllers.model



import no.nav.tsm.mottak.sykmelding.model.AktivitetIkkeMulig
import no.nav.tsm.mottak.sykmelding.model.AvsenderSystem
import no.nav.tsm.mottak.sykmelding.model.Behandler
import no.nav.tsm.mottak.sykmelding.model.EnArbeidsgiver
import no.nav.tsm.mottak.sykmelding.model.MedisinskVurdering
import no.nav.tsm.mottak.sykmelding.model.Pasient
import no.nav.tsm.mottak.sykmelding.model.RuleType
import no.nav.tsm.mottak.sykmelding.model.SignerendeBehandler
import no.nav.tsm.mottak.sykmelding.model.Sykmelding
import no.nav.tsm.mottak.sykmelding.model.SykmeldingMetadata
import no.nav.tsm.mottak.sykmelding.model.SykmeldingRecord
import no.nav.tsm.mottak.sykmelding.model.ValidationResult
import no.nav.tsm.mottak.sykmelding.model.metadata.HelsepersonellKategori
import no.nav.tsm.mottak.sykmelding.model.metadata.Navn
import no.nav.tsm.mottak.sykmelding.model.metadata.Utenlandsk
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

fun createNewSykmelding() : SykmeldingRecord
{
    return SykmeldingRecord(
        sykmelding = Sykmelding(
            id = UUID.randomUUID().toString(),
            metadata = SykmeldingMetadata(mottattDato = OffsetDateTime.now(), genDate = OffsetDateTime.now(), behandletTidspunkt = OffsetDateTime.now(), regelsettVersjon = null, avsenderSystem = AvsenderSystem(navn = "", versjon =""), strekkode = null),
            pasient = Pasient(navn = null, navKontor = null, navnFastlege = null, fnr = "12345678901", kontaktinfo = emptyList()),
            medisinskVurdering = MedisinskVurdering(hovedDiagnose = null, biDiagnoser = null, svangerskap = true, yrkesskade = null, skjermetForPasient = true, syketilfelletStartDato = null, annenFraversArsak = null),
            aktivitet = listOf(AktivitetIkkeMulig(fom = LocalDate.now(), tom = LocalDate.now(), medisinskArsak = null, arbeidsrelatertArsak = null)),
            behandler = Behandler(navn = Navn(fornavn = "fdf", mellomnavn = null, etternavn = "sdgfgfd"), adresse = null, ids = emptyList(), kontaktinfo = emptyList()),
            arbeidsgiver = EnArbeidsgiver(meldingTilArbeidsgiver = null, tiltakArbeidsplassen = null),
            signerendeBehandler = SignerendeBehandler(ids = emptyList(), helsepersonellKategori = HelsepersonellKategori.HELSEFAGARBEIDER),
            prognose = null,
            tiltak = null,
            bistandNav = null,
            tilbakedatering = null,
            utdypendeOpplysninger = null
            ),
        metadata = Utenlandsk(land = "", journalPostId = "75467656"),
        validation = ValidationResult(RuleType.OK, rules = emptyList(), timestamp = OffsetDateTime.now())
    )
}

internal fun Int.januar(year: Int) = LocalDate.of(year, 1, this)
