package no.nav.tsm.mottak.sykmelding.kafka.util

import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import org.apache.kafka.common.serialization.Serializer
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonMapperBuilder

class SykmeldingRecordSerializer : Serializer<SykmeldingRecord> {
    private val objectMapper: ObjectMapper = jacksonMapperBuilder().build()

    override fun serialize(topic: String, data: SykmeldingRecord?): ByteArray? {
        if (data != null) {
            return objectMapper.writeValueAsBytes(data)
        }
        return null
    }
}
