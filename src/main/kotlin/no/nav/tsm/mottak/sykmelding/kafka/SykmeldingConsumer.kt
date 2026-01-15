package no.nav.tsm.mottak.sykmelding.kafka

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.tsm.mottak.sykmelding.service.SykmeldingService
import no.nav.tsm.sykmelding.input.core.model.SykmeldingModule
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.sykmeldingObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class SykmeldingConsumer(
    private val sykmeldingService: SykmeldingService,
) {


    val objectMapper =
        jacksonObjectMapper().apply {
            registerModule(SykmeldingModule())
            registerModule(JavaTimeModule())
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }

  @KafkaListener(
        topics = ["\${spring.kafka.topics.sykmeldinger-input}"],
        groupId = "regulus-maximus-consumer",
        containerFactory = "containerFactory",
        batch = "false"
    )
    fun consume(record: ConsumerRecord<String, ByteArray?>) {
        val sykmelding = record.value()?.let { objectMapper.readValue(it, SykmeldingRecord::class.java) }
        sykmeldingService.updateSykmelding(record.key(), sykmelding, record.headers())
    }
}
