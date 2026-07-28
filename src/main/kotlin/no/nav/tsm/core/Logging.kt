package no.nav.tsm.core

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.tsm.sykmelding.input.core.model.SykmeldingModule

private val logObjectMapper: ObjectMapper =
    ObjectMapper().apply {
        registerKotlinModule()
        registerModule(SykmeldingModule())
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }

fun Any.logData(): String {
    return logObjectMapper.writeValueAsString(this)
}
