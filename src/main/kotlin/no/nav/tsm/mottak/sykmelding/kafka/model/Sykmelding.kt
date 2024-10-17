package no.nav.tsm.mottak.sykmelding.kafka.model

import no.nav.tsm.mottak.sykmelding.kafka.model.metadata.*
import no.nav.tsm.mottak.sykmelding.kafka.model.metadata.UtenlandskSykmeldingInfo
import java.time.LocalDate
import java.time.OffsetDateTime

data class SykmeldingMedBehandlingsutfall(
    val sykmelding: Sykmelding,
    val validation: ValidationResult,
    val meldingsInformasjon: Meldingsinformasjon,
)

enum class SykmeldingType {
    SYKMELDING,
    UTENLANDSK_SYKMELDING
}

sealed interface ISykmelding {
    val type: SykmeldingType
    val id: String
    val metadata: SykmeldingMetadata
    val pasient: Pasient
    val medisinskVurdering: MedisinskVurdering
    val aktivitet: List<Aktivitet>
}
data class UtenlandskSykmelding(
    override val id: String,
    override val metadata: SykmeldingMetadata,
    override val pasient: Pasient,
    override val medisinskVurdering: MedisinskVurdering,
    override val aktivitet: List<Aktivitet>,
    val utenlandskInfo: UtenlandskSykmeldingInfo
) : ISykmelding {
    override val type = SykmeldingType.UTENLANDSK_SYKMELDING
}

data class Sykmelding(
    override val id: String,
    override val metadata: SykmeldingMetadata,
    override val pasient: Pasient,
    override val medisinskVurdering: MedisinskVurdering,
    override val aktivitet: List<Aktivitet>,
    val behandler: Behandler,
    val arbeidsgiver: ArbeidsgiverInfo,
    val signerendeBehandler: SignerendeBehandler,
    val prognose: Prognose?,
    val tiltak: Tiltak?,
    val bistandNav: BistandNav?,
    val tilbakedatering: Tilbakedatering?,
    val generatedDate: OffsetDateTime,
    val utdypendeOpplysninger: Map<String, Map<String, SporsmalSvar>>?,
) : ISykmelding {
    override val type = SykmeldingType.SYKMELDING
}

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




