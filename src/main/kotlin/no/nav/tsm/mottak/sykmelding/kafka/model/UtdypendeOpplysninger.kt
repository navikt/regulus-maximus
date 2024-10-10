package no.nav.tsm.mottak.sykmelding.kafka.model

data class SporsmalSvar(
    val sporsmal: String?, val svar: String, val restriksjoner: List<SvarRestriksjon>
)

enum class SvarRestriksjon{
    SKJERMET_FOR_ARBEIDSGIVER, SKJERMET_FOR_PASIENT, SKJERMET_FOR_NAV,
}
