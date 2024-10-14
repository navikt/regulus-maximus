package no.nav.tsm.sykmelding.metadata

import java.time.LocalDate

enum class PersonIdType {
    FNR,
    DNR,
    HNR,
    HPR,
    HER,
    PNR,
    SEF,
    DKF,
    SSN,
    FPN,
    XXX
}
enum class Kjonn {
    MANN,
    KVINNE,
    USPESIFISERT,
    IKKE_OPPGITT;

    companion object {
        fun parse(v: String?) : Kjonn {
            return when (v) {
                "1" -> MANN
                "2" -> KVINNE
                "9" -> USPESIFISERT
                null -> IKKE_OPPGITT
                else -> throw IllegalArgumentException("Ukjent kjønn: $v")
            }
        }
    }
}

data class Navn(
    val fornavn: String, val mellomnavn: String?, val etternavn: String
)

data class PersonId(
    val id: String,
    val type: PersonIdType,
)


data class Pasient (
    val ids: List<PersonId>,
    val navn: Navn?,
    val fodselsdato: LocalDate?,
    val kjonn: Kjonn?,
    val nasjonalitet: String?,
    val adresse: Adresse?,
    val kontaktinfo: List<Kontaktinfo>,
)

enum class HelsepersonellKategori {
    HELSESEKRETAR,
    KIROPRAKTOR,
    LEGE,
    MANUELLTERAPEUT,
    TANNLEGE,
    USPESIFISERT,
    IKKE_OPPGITT;

    companion object {
        fun parse(v: String?): HelsepersonellKategori {
            return when(v) {
                "HE" ->	HELSESEKRETAR
                "KI" ->	KIROPRAKTOR
                "LE" ->	LEGE
                "MT" ->	MANUELLTERAPEUT
                "TL" ->	TANNLEGE
                "XX" ->  USPESIFISERT
                null -> IKKE_OPPGITT
                else -> throw IllegalArgumentException("Ukjent helsepersonellkategori: $v")
            }
        }
    }
}

enum class RolleTilPasient {
    JOURNALANSVARLIG,
    FASTLEGE,
    IKKE_OPPGITT;

    companion object {
        fun parse(v: String?): RolleTilPasient {
           return when (v) {
               "4" -> JOURNALANSVARLIG
               "6" -> FASTLEGE
               null -> IKKE_OPPGITT
               else -> throw IllegalArgumentException("Ukjent rolle til pasient: $v")
           }
        }
    }
}

data class Helsepersonell(
    val ids: List<PersonId>,
    val navn: Navn?,
    val fodselsdato: LocalDate?,
    val kjonn: Kjonn?,
    val nasjonalitet: String?,
    val adresse: Adresse?,
    val kontaktinfo: List<Kontaktinfo>,
    val helsepersonellKategori: HelsepersonellKategori,
    val rolleTilPasient: RolleTilPasient,
)
