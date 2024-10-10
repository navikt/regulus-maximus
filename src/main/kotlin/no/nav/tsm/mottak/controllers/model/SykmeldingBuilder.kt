package no.nav.tsm.mottak.controllers.model

import no.nav.tsm.mottak.sykmelding.kafka.model.*
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

val sykmeldingMedBehandlingsutfall = SykmeldingMedBehandlingsutfall(
    sykmelding = Sykmelding(
        id = UUID.randomUUID().toString(),
        metadata = SykmeldingMetadata(
            msgId = null,
            regelsettVersjon = "1",
            partnerreferanse = null,
            avsenderSystem = AvsenderSystem("", ""),
            mottattDato = OffsetDateTime.now(),
            behandletTidspunkt = OffsetDateTime.now(),
        ),
        pasient = Person(ident = "", navn = null),
        behandler = Behandler(
            person = Person(ident = "", navn = null),
            adresse = Adresse(null, null, null, null, null),
            kontaktInfo = emptyList()
        ),
        arbeidsgiver = EnArbeidsgiver(null, null),
        medisinskVurdering = MedisinskVurdering(hovedDiagnose = null, biDiagnoser = null, svangerskap = false, yrkesskade = false, yrkesskadeDato = null, skjermetForPasient = false, syketilfelletStartDato = null, annenFraversArsak = null),
        prognose = Prognose(arbeidsforEtterPeriode = false, null, null),
        tiltak = null,
        bistandNav = null,
        tilbakedatering = null,
        aktivitet = listOf(AktivitetIkkeMulig(medisinskArsak = MedisinskArsak(null, MedisinskArsakType.ANNET), null, fom = 1.januar(2023), tom = 31.januar(2023))),
        utdypendeOpplysninger = emptyMap(),
        generatedDate = OffsetDateTime.now()
        ),
    validation = ValidationResult(
        status = RuleType.OK,
        timestamp = OffsetDateTime.now(),
        rules = emptyList()
    ),
    kilde = SykmeldingKilde.PAPIR
)

internal fun Int.januar(year: Int) = LocalDate.of(year, 1, this)
