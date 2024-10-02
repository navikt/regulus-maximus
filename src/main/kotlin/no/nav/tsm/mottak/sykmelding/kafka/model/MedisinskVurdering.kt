package no.nav.tsm.mottak.sykmelding.kafka.model

import java.time.LocalDate

enum class DiagnoseSystem {
    ICPC2, ICD10,
}

data class DiagnoseInfo(
    val system: DiagnoseSystem,
    val kode: String,
    val tekst: String?, // Fjernes?
)

enum class MedisinskArsakType {
    TILSTAND_HINDRER_AKTIVITET, AKTIVITET_FORVERRER_TILSTAND, AKTIVITET_FORHINDRER_BEDRING, ANNET,
}

enum class ArbeidsrelatertArsakType {
    MANGLENDE_TILRETTELEGGING, ANNET,
}

enum class AnnenFravarArsakType {
    GODKJENT_HELSEINSTITUSJON, BEHANDLING_FORHINDRER_ARBEID, ARBEIDSRETTET_TILTAK, MOTTAR_TILSKUDD_GRUNNET_HELSETILSTAND, NODVENDIG_KONTROLLUNDENRSOKELSE, SMITTEFARE, ABORT, UFOR_GRUNNET_BARNLOSHET, DONOR, BEHANDLING_STERILISERING,
}

data class AnnenFraverArsak(
    val beskrivelse: String?, val arsak: AnnenFravarArsakType // TODO: Sjekk om det bare er en av disse som kan være satt
)

data class MedisinskArsak(
    val beskrivelse: String?, val arsak: MedisinskArsakType
)

data class ArbeidsrelatertArsak(
    val beskrivelse: String?, val arsak: ArbeidsrelatertArsakType
)

data class Yrkesskade(
    val yrkesskadeDato: LocalDate?
)

data class MedisinskVurdering(
    val hovedDiagnose: DiagnoseInfo?,
    val biDiagnoser: List<DiagnoseInfo>?,
    val svangerskap: Boolean,
    val yrkesskade: Boolean,
    val yrkesskadeDato: LocalDate?, // Sjekke i spec om yrkesskadedato må være satt og kanksje egen klasse yrkessakde
    val skjermetForPasient: Boolean,
    val syketilfelletStartDato: LocalDate?,
    val annenFraversArsak: AnnenFraverArsak?,
)
