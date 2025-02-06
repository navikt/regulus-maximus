package no.nav.tsm.mottak.sykmelding.kafka.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.tsm.mottak.sykmelding.kafka.model.SykmeldingRecord
import org.apache.kafka.common.serialization.Serializer

class SykmeldingRecordSerializer : Serializer<SykmeldingRecord> {
    private val objectMapper: ObjectMapper =
        jacksonObjectMapper().apply {
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }

    override fun serialize(topic: String, data: SykmeldingRecord?): ByteArray? {
        if (data != null) {
            return objectMapper.writeValueAsBytes(data)
        }
        return null
    }
}
