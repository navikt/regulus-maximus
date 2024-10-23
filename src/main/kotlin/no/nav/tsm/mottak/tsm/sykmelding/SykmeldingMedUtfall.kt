package no.nav.tsm.mottak.tsm.sykmelding

import no.nav.tsm.mottak.sykmelding.kafka.model.ISykmelding

data class SykmeldingMedUtfall(
    val sykmelding: ISykmelding,
)
