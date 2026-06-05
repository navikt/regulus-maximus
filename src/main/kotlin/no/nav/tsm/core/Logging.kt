package no.nav.tsm.core

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.tsm.sykmelding.input.core.model.SykmeldingModule
import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun logger(): Logger =
    LoggerFactory.getLogger(
        StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).callerClass
    )

fun teamLogger(): Logger =
    LoggerFactory.getLogger(
        "teamlog.${StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).callerClass}"
    )

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
