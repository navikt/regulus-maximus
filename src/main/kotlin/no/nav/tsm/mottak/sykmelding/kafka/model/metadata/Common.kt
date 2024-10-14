package no.nav.tsm.sykmelding.metadata

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
    UKJENT;

    companion object {
        fun parse(v: String): AdresseType {
            return when (v) {
                "BAD" -> UBRUKELIG_ADRESSE
                "H" -> BOSTEDSADRESSE
                "HP" -> FOLKEREGISTERADRESSE
                "HV" -> FERIEADRESSE
                "INV" -> FAKTURERINGSADRESSE
                "PST" -> POSTADRESSE
                "RES" -> BESOKSADRESSE
                "TMP" -> MIDLERTIDIG_ADRESSE
                "WP" -> ARBEIDSADRESSE
                else -> throw IllegalArgumentException("Ukjent adressestype: $v")
            }
        }
    }
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
    TLF;

    companion object {
        fun parse(v: String): KontaktinfoType {
            return when (v) {
                "AS" -> TELEFONSVARER
                "EC" -> NODNUMMER
                "F" -> FAX_TELEFAKS
                "H" -> HJEMME_ELLER_UKJENT
                "HP" -> HOVEDTELEFON
                "HV" -> FERIETELEFON
                "MC" -> MOBILTELEFON
                "PG" -> PERSONSOKER
                "WC" -> ARBEIDSPLASS_SENTRALBORD
                "WD" -> ARBEIDSPLASS_DIREKTENUMMER
                "WP" -> ARBEIDSPLASS
                else -> throw IllegalArgumentException("Ukjent kontaktinfotype: $v")
            }
        }
    }
}
