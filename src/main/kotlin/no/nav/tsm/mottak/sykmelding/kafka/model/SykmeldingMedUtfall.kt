package no.nav.tsm.mottak.sykmelding.kafka.model

data class SykmeldingMedUtfall(
    val sykmeldingInput: SykmeldingInput,
    val utfall: String,
)
