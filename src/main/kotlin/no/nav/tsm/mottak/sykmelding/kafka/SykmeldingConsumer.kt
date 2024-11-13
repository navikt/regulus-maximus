package no.nav.tsm.mottak.sykmelding.kafka

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.tsm.mottak.service.SykmeldingService
import no.nav.tsm.mottak.sykmelding.kafka.model.SykmeldingMedBehandlingsutfall
import no.nav.tsm.mottak.sykmelding.kafka.util.SykmeldingModule
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class SykmeldingConsumer(
    private val kafkaTemplate: KafkaProducer<String, SykmeldingMedBehandlingsutfall>,
    private val sykmeldingService: SykmeldingService,
    @Value("\${spring.kafka.topics.sykmeldinger-output}") private val sykmeldingOutputTopic: String
) {
    private val logger = LoggerFactory.getLogger(SykmeldingConsumer::class.java)

    @KafkaListener(
        topics = ["\${spring.kafka.topics.sykmeldinger-input}"],
        groupId = "regulus-maximus",
        containerFactory = "containerFactory",
        batch = "true"
    )
    fun consume(records: List<ConsumerRecord<String, SykmeldingMedBehandlingsutfall>>) {
        try {
            records.forEach { record ->
                if (record.value() != null) {
                    val sykmelding = record.value()
                    sykmeldingService.saveSykmelding(sykmelding)
                    sendToTsmSykmelding(sykmelding)
                } else {
                    sykmeldingService.delete(record.key())
                    tombStone(record.key())

                }
            }
        } catch (e: Throwable) {
            logger.error("Failed to read message fra topic ", e)
            throw e
        }
    }

    private fun sendToTsmSykmelding(sykmelding: SykmeldingMedBehandlingsutfall) {
        try {
            kafkaTemplate.send(
                ProducerRecord(
                    sykmeldingOutputTopic,
                    sykmelding.sykmelding.id,
                    SykmeldingMedBehandlingsutfall(
                        sykmelding = sykmelding.sykmelding,
                        metadata = sykmelding.metadata,
                        validation = sykmelding.validation
                    )
                )
            ).get()
        } catch (toSykmeldingException: Exception) {
            logger.error("Failed to publish sykmelding to tsm.sykmelding", toSykmeldingException)
            throw toSykmeldingException
        }
    }

    private fun tombStone(sykmeldingId: String) {
        try {
            kafkaTemplate.send(
                ProducerRecord(
                    sykmeldingOutputTopic,
                    sykmeldingId,
                    null
                )
            )
        } catch (toTombstoneException: Exception) {
            logger.error("Failed to tombstone sykmelding to tsm.sykmelding", toTombstoneException)
            throw toTombstoneException
        }
    }
}


val objectMapper: ObjectMapper =
    ObjectMapper().apply {
        registerKotlinModule()
        registerModule(SykmeldingModule())
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }
