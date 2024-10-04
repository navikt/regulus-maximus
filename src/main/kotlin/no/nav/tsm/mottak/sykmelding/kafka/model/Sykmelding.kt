package no.nav.tsm.mottak.sykmelding.kafka.model

import java.time.LocalDate
import java.time.OffsetDateTime

data class SykmeldingMedBehandlingsutfall(
    val sykmelding: Sykmelding,
    val validation: ValidationResult,
    val kilde: SykmeldingKilde,
)
enum class SykmeldingKilde {
    ELEKTRONISK, PAPIR, UTENLANDSK_PAPIR, UTENLANDS_NAV_NO, UTENLANDS_RINA
}
data class Sykmelding(
    val id: String,
    val metadata: SykmeldingMetadata,
    val pasient: Person,
    val behandler: Behandler,
    val arbeidsgiver: ArbeidsgiverInfo,
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
    val genDate: OffsetDateTime, // Todo endre navn?
    val behandletTidspunkt: OffsetDateTime,
)

data class Navn(
    val fornavn: String, val etternavn: String
)

data class Person(
    val ident: String, val navn: Navn?
)


data class Adresse(
    val gateAdresse: String?,
    val postnummer: String?,
    val land: String?,
    val kommune: String?,
    val postbox: String?,
)

data class Kontaktinfo( // TODO: sjekk hva det kan være
    val type: String, val verdi: String
)

data class Behandler(
    val person: Person,
    val adresse: Adresse,
    val kontaktInfo: List<Kontaktinfo>
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



