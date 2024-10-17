package no.nav.tsm.mottak.sykmelding.kafka.model.metadata

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id
import java.time.OffsetDateTime

enum class MetadataType {
    EMOTTAK,
    ENKEL,
    UTENLANDSK_SYKMELDING,
    PAPIRSYKMELDING,
}

@JsonSubTypes(
    Type(Papirsykmelding::class, name = "PAPIRSYKMELDING"),
    Type(Utenlandsk::class, name = "UTENLANDSK_SYKMELDING"),
    Type(EmottakEnkel::class, name = "ENKEL"),
    Type(EDIEmottak::class, name = "EDI_EMOTTAK"),
)
@JsonTypeInfo(use = Id.NAME, include = PROPERTY, property = "type")
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
    override val type = MetadataType.PAPIRSYKMELDING
}

data class Utenlandsk(
    override val msgInfo: MeldingMetadata,
    override val sender: Organisasjon,
    override val receiver: Organisasjon,
    val utenlandskSykmelding: UtenlandskSykmelding
) : Meldingsinformasjon
{
    override val vedlegg = null
    override val type: MetadataType = MetadataType.UTENLANDSK_SYKMELDING
}


data class UtenlandskSykmelding(
    val land: String,
    val folkeRegistertAdresseErBrakkeEllerTilsvarende: Boolean,
    val erAdresseUtland: Boolean?,
)

data class EmottakEnkel(
    override val msgInfo: MeldingMetadata,
    override val sender: Organisasjon,
    override val receiver: Organisasjon,
    override val vedlegg: List<String>?,
) : Meldingsinformasjon {
    override val type = MetadataType.ENKEL
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

enum class AckType {
    JA,
    NEI,
    KUN_VED_FEIL,
    IKKE_OPPGITT,
    UGYLDIG;
    companion object {
        fun parse(value: String?): AckType {
            return when (value) {
                null -> IKKE_OPPGITT
                "J" -> JA
                "N" -> NEI
                "F" -> KUN_VED_FEIL
                "" -> UGYLDIG
                else -> throw IllegalArgumentException("Unrecognized ack type: $value")
            }
        }
    }
}
data class Ack(
    val ackType: AckType,
)