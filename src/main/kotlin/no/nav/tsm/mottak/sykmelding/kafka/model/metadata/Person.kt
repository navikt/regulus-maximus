package no.nav.tsm.mottak.sykmelding.kafka.model.metadata

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
    XXX,
    DUF,
    IKKE_OPPGITT,
    UGYLDIG;

}
enum class Kjonn {
    MANN,
    KVINNE,
    USPESIFISERT,
    IKKE_OPPGITT,
    UGYLDIG;

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
    FYSIOTERAPEUT,
    SYKEPLEIER,
    HJELPEPLEIER,
    HELSEFAGARBEIDER,
    USPESIFISERT,
    UGYLDIG,
    JORDMOR,
    AUDIOGRAF,
    NAPRAPAT,
    AMBULANSEARBEIDER,
    PSYKOLOG,
    FOTTERAPEUT,
    TANNHELSESEKRETAR,
    IKKE_OPPGITT;

}

enum class RolleTilPasient {
    JOURNALANSVARLIG,
    FASTLEGE,
    IKKE_OPPGITT;

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
