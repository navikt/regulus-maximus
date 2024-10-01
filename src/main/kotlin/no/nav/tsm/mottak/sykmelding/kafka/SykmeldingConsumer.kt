package no.nav.tsm.mottak.sykmelding.kafka

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.tsm.mottak.config.KafkaConfigProperties
import no.nav.tsm.mottak.service.SykmeldingService
import no.nav.tsm.mottak.sykmelding.kafka.model.SykmeldingMedUtfall
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class SykmeldingConsumer(
    private val kafkaConfigProperties: KafkaConfigProperties,
    private val sykmeldingService: SykmeldingService,
) {
    private val logger = LoggerFactory.getLogger(SykmeldingConsumer::class.java)

    @KafkaListener(topics = ["\${spring.kafka.topics.mottatt-sykmelding}"], groupId = "\${spring.kafka.group-id}")
    fun consume(cr: ConsumerRecord<String, String>?) {
        if (cr != null){
            val sykmelding = objectMapper.readValue(cr.value(), SykmeldingMedUtfall::class.java)
            logger.info("Received message from topic: ${cr.value()}")
            sykmeldingService.saveSykmelding(sykmelding).subscribe(
                { savedEntity -> logger.info("Sykmelding saved: $savedEntity") },
                { error -> logger.error("Failed to save sykmelding", error) }
            )
        }

        // her skal videre funksjonalitet ligge

    }
}



val objectMapper: ObjectMapper =
    ObjectMapper().apply {
        registerKotlinModule()
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }
