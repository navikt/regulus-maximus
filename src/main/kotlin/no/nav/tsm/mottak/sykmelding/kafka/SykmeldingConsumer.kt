package no.nav.tsm.mottak.sykmelding.kafka

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotlinx.coroutines.runBlocking
import no.nav.tsm.mottak.config.KafkaConfigProperties
import no.nav.tsm.mottak.service.SykmeldingService
import no.nav.tsm.mottak.sykmelding.kafka.model.SykmeldingMedBehandlingsutfall
import no.nav.tsm.mottak.sykmelding.kafka.util.SykmeldingModule
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class SykmeldingConsumer(
    private val kafkaConfigProperties: KafkaConfigProperties,
    private val kafkaTemplate: KafkaTemplate<String, SykmeldingMedBehandlingsutfall>,
    private val sykmeldingService: SykmeldingService,
) {
    private val logger = LoggerFactory.getLogger(SykmeldingConsumer::class.java)


    @KafkaListener(topics = ["\${spring.kafka.topics.mottatt-sykmelding}"], groupId = "\${spring.kafka.group-id}")
    fun consume(cr: ConsumerRecord<String, String>?) {
        try {
            if (cr != null) {
                val sykmelding = objectMapper.readValue(cr.value(), SykmeldingMedBehandlingsutfall::class.java)
                // if (cr.headers())
                logger.info("Received message from topic: ${cr.value()}")
                runBlocking {
                    sykmeldingService.saveSykmelding(sykmelding)
                }

                kafkaTemplate.send(kafkaConfigProperties.topics.mottattSykmelding, SykmeldingMedBehandlingsutfall(sykmelding = sykmelding.sykmelding, validation = sykmelding.validation, kilde = sykmelding.kilde))
            }
        } catch (e: Throwable) {
            logger.error("Kunne ikke lese melding fra topic ", e)
            throw e
        }
          catch (ex: Exception) {
             logger.error("Failed to publish sykmelding to tsm.sykmelding", ex)
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
