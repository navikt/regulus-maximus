package no.nav.tsm.sykmelding.metadata

import no.nav.tsm.mottak.sykmelding.kafka.model.metadata.Organisasjon
import java.time.OffsetDateTime

enum class MetadataType {
    EMOTTAK,
    EMOTTAK_ENKEL,
    UTENLANDSK_SYKMELDING,
    PAPIRSYKMELDING_SYKMELDING,
}

sealed interface Meldingsinformasjon {
    val msgInfo: MeldingMetadata
    val sender: Organisasjon
    val receiver: Organisasjon
    val type: MetadataType
    val vedlegg: List<String>?
}

data class Papirsykmelding(
    override val msgInfo: MeldingMetadata,
    override val sender: Organisasjon,
    override val receiver: Organisasjon
) : Meldingsinformasjon {
    override val vedlegg = null
    override val type = MetadataType.PAPIRSYKMELDING_SYKMELDING
}

data class UtenlandskSykmelding(
    val land: String,
    val folkeRegistertAdresseErBrakkeEllerTilsvarende: Boolean,
    val erAdresseUtland: Boolean?,
)
data class Utenlandsk(
    override val msgInfo: MeldingMetadata,
    override val sender: Organisasjon,
    override val receiver: Organisasjon,
    val utenlandskSykmelding: UtenlandskSykmelding
) : Meldingsinformasjon {
    override val vedlegg = null
    override val type: MetadataType = MetadataType.UTENLANDSK_SYKMELDING
}

data class EmottakEnkel(
    override val msgInfo: MeldingMetadata,
    override val sender: Organisasjon,
    override val receiver: Organisasjon,
    override val vedlegg: List<String>?,
) : Meldingsinformasjon {
    override val type = MetadataType.EMOTTAK_ENKEL
}

data class EDIEmottak(
    val mottakenhetBlokk: MottakenhetBlokk,
    override val msgInfo: MeldingMetadata,
    override val sender: Organisasjon,
    override val receiver: Organisasjon,
    val pasient: Pasient?,
    override val vedlegg: List<String>?,
) : Meldingsinformasjon {
    override val type = MetadataType.EMOTTAK
}

enum class Meldingstype {
    SYKMELDING;

    companion object {
        fun parse(v: String): Meldingstype = when (v) {
            "SYKMELD" -> SYKMELDING
            else -> throw IllegalArgumentException("Ukjent meldingstype: $v")
        }
    }
}


data class MeldingMetadata(
    val type: Meldingstype,
    val genDate: OffsetDateTime,
    val msgId: String,
    val migVersjon: String?,
)

data class MottakenhetBlokk(
    val ediLogid: String,
    val avsender: String,
    val ebXMLSamtaleId: String,
    val mottaksId: String,
    val meldingsType: String,
    val avsenderRef: String,
    val avsenderFnrFraDigSignatur: String,
    val mottattDato: OffsetDateTime,
    val orgnummer: String,
    val avsenderOrgNrFraDigSignatur: String,
    val partnerReferanse: String,
    val herIdentifikator: String,
    val ebRole: String,
    val ebService: String,
    val ebAction: String,
)
