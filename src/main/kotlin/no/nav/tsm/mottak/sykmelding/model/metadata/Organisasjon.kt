package no.nav.tsm.mottak.sykmelding.model.metadata

enum class OrgIdType {
    AKO,
    APO,
    AVD,
    ENH,
    HER,
    LAV,
    LIN,
    LOK,
    NPR,
    RSH,
    SYS,
    UGYLDIG;
}

enum class OrganisasjonsType {
    PRIVATE_SPESIALISTER_MED_DRIFTSAVTALER,
    TANNLEGE_TANNHELSE,
    IKKE_OPPGITT,
    UGYLDIG;
}

data class OrgId(
    val id: String,
    val type: OrgIdType,
)

data class Organisasjon(
    val navn: String?,
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
