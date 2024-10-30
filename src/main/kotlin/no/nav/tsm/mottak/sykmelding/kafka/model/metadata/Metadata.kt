package no.nav.tsm.mottak.sykmelding.kafka.model.metadata

import java.time.OffsetDateTime
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id

enum class MetadataType {
    ENKEL,
    EMOTTAK,
    UTENLANDSK_SYKMELDING,
    PAPIRSYKMELDING,
}

@JsonSubTypes(
    Type(Papirsykmelding::class, name = "PAPIRSYKMELDING"),
    Type(Utenlandsk::class, name = "UTENLANDSK_SYKMELDING"),
    Type(EmottakEnkel::class, name = "ENKEL"),
    Type(EDIEmottak::class, name = "EMOTTAK"),
)
@JsonTypeInfo(use = Id.NAME, include = PROPERTY, property = "type")
sealed interface Meldingsinformasjon {
    val type: MetadataType
    val vedlegg: List<String>?
}

data class Papirsykmelding(
    val msgInfo: MeldingMetadata,
    val sender: Organisasjon,
    val receiver: Organisasjon,
    val journalPostId: String,
) : Meldingsinformasjon {
    override val vedlegg = null
    override val type = MetadataType.PAPIRSYKMELDING
}

data class Utenlandsk(
    val land: String,
    val journalPostId: String,
) : Meldingsinformasjon {
    override val vedlegg = null
    override val type: MetadataType = MetadataType.UTENLANDSK_SYKMELDING
}

data class EmottakEnkel(
    val msgInfo: MeldingMetadata,
    val sender: Organisasjon,
    val receiver: Organisasjon,
    override val vedlegg: List<String>?,
) : Meldingsinformasjon {
    override val type = MetadataType.ENKEL
}

enum class AckType {
    JA,
    NEI,
    KUN_VED_FEIL,
    IKKE_OPPGITT,
    UGYLDIG;

}
data class Ack(
    val ackType: AckType,
)

data class EDIEmottak(
    val mottakenhetBlokk: MottakenhetBlokk,
    val ack: Ack,
    val msgInfo: MeldingMetadata,
    val sender: Organisasjon,
    val receiver: Organisasjon,
    val pasient: Pasient?,
    override val vedlegg: List<String>?,
) : Meldingsinformasjon {
    override val type = MetadataType.EMOTTAK
}

enum class Meldingstype {
    SYKMELDING;
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
    val mottaksId: String?,
    val meldingsType: String,
    val avsenderRef: String,
    val avsenderFnrFraDigSignatur: String?,
    val mottattDato: OffsetDateTime,
    val orgnummer: String?,
    val avsenderOrgNrFraDigSignatur: String?,
    val partnerReferanse: String,
    val herIdentifikator: String,
    val ebRole: String,
    val ebService: String,
    val ebAction: String,
)
