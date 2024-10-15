package no.nav.tsm.mottak.tsm.sykmelding

import no.nav.tsm.mottak.sykmelding.kafka.model.Sykmelding

data class SykmeldingMedUtfall(
    val sykmelding: Sykmelding,
)
