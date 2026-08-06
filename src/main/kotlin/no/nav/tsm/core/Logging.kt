package no.nav.tsm.core

import no.nav.tsm.sykmelding.input.core.model.SykmeldingModule
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonMapperBuilder

private val logObjectMapper: ObjectMapper =
    jacksonMapperBuilder().addModules(SykmeldingModule()).build()

fun Any.logData(): String {
    return logObjectMapper.writeValueAsString(this)
}
