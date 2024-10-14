package no.nav.tsm.mottak.sykmelding.kafka.model.metadata

import no.nav.tsm.mottak.sykmelding.kafka.model.metadata.Helsepersonell

enum class OrgIdType {
    ENH,
    RSH,
    HER,
    NPR,
    SYS,
    APO,
    AKO,
    LIN,
    LAV,
    LOK;
}

enum class OrganisasjonsType {
    PRIVATE_SPESIALISTER_MED_DRIFTSAVTALER,
    TANNLEGE_TANNHELSE,
    IKKE_OPPGITT;

    companion object {
        fun parse(v: String?): OrganisasjonsType {
            return when(v) {
                "4" -> PRIVATE_SPESIALISTER_MED_DRIFTSAVTALER
                "110" -> TANNLEGE_TANNHELSE
                null -> IKKE_OPPGITT
                else -> throw IllegalArgumentException("Ukjent organisasjonstype: $v")
            }
        }
    }
}

data class OrgId(
    val id: String,
    val type: OrgIdType,
)

data class Organisasjon(
    val navn: String,
    val type: OrganisasjonsType,
    val ids: List<OrgId>,
    val adresse: Adresse?,
    val kontaktinfo: List<Kontaktinfo>?,
    val underOrganisasjon: UnderOrganisasjon?,
    val helsepersonell: Helsepersonell?,
)

data class UnderOrganisasjon(
    val navn: String,
    val type: OrganisasjonsType,
    val adresse: Adresse?,
    val kontaktinfo: List<Kontaktinfo>,
    val ids: List<OrgId>,
)
