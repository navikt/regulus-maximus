package no.nav.tsm.mottak.sykmelding.kafka.model.metadata

enum class AdresseType {
    BOSTEDSADRESSE,
    FOLKEREGISTERADRESSE,
    FERIEADRESSE,
    FAKTURERINGSADRESSE,
    POSTADRESSE,
    BESOKSADRESSE,
    MIDLERTIDIG_ADRESSE,
    ARBEIDSADRESSE,
    UBRUKELIG_ADRESSE,
    UKJENT,
    UGYLDIG;
}

data class Adresse(
    val type: AdresseType,
    val gateadresse: String?,
    val postnummer: String?,
    val poststed: String?,
    val postboks: String?,
    val kommune: String?,
    val land: String?,
)

data class Kontaktinfo(
    val type: KontaktinfoType, val value: String
)

enum class KontaktinfoType {
    TELEFONSVARER,
    NODNUMMER,
    FAX_TELEFAKS,
    HJEMME_ELLER_UKJENT,
    HOVEDTELEFON,
    FERIETELEFON,
    MOBILTELEFON,
    PERSONSOKER,
    ARBEIDSPLASS_SENTRALBORD,
    ARBEIDSPLASS_DIREKTENUMMER,
    ARBEIDSPLASS,
    TLF,
    IKKE_OPPGITT,
    UGYLDIG;
}
