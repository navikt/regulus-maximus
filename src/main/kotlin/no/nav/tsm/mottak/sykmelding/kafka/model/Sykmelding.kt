package no.nav.tsm.mottak.sykmelding.kafka.model

import no.nav.tsm.mottak.sykmelding.kafka.model.metadata.*
import java.time.LocalDate
import java.time.OffsetDateTime

data class SykmeldingMedBehandlingsutfall(
    val sykmelding: Sykmelding,
    val validation: ValidationResult,
    val meldingsInformasjon: Meldingsinformasjon,
)

data class Sykmelding(
    val id: String,
    val metadata: SykmeldingMetadata,
    val generatedDate: OffsetDateTime,
    val pasient: Pasient,
    val behandler: Behandler,
    val arbeidsgiver: ArbeidsgiverInfo,
    val signerendeBehandler: SignerendeBehandler,
    val medisinskVurdering: MedisinskVurdering,
    val prognose: Prognose?,
    val tiltak: Tiltak?,
    val bistandNav: BistandNav?,
    val tilbakedatering: Tilbakedatering?,
    val aktivitet: List<Aktivitet>,
    val utdypendeOpplysninger: Map<String, Map<String, SporsmalSvar>>?,
)

data class SykmeldingMetadata(
    val msgId: String?,
    val regelsettVersjon: String,
    val partnerreferanse: String?,
    val avsenderSystem: AvsenderSystem,
    val mottattDato: OffsetDateTime,
    val behandletTidspunkt: OffsetDateTime,
)

data class Behandler(
    val navn: Navn,
    val ids: List<PersonId>,
    val adresse: Adresse?,
    val kontaktInfo: List<Kontaktinfo>
)

data class SignerendeBehandler(
    val ids: List<PersonId>,
    val helsepersonellKategori: HelsepersonellKategori,
)

data class BistandNav(
    val bistandUmiddelbart: Boolean,
    val beskrivBistand: String?,
)

data class Tiltak(
    val tiltakNAV: String?,
    val andreTiltak: String?,
)

data class Prognose(
    val arbeidsforEtterPeriode: Boolean,
    val hensynArbeidsplassen: String?,
    val arbeid: IArbeid?,
)

data class Tilbakedatering(
    val kontaktDato: LocalDate?,
    val begrunnelse: String?,
)
data class AvsenderSystem(val navn: String, val versjon: String)




