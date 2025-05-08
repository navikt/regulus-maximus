package no.nav.tsm.mottak.sykmelding.kafka

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.tsm.mottak.db.SykmeldingDBMappingException
import no.nav.tsm.mottak.pdl.PersonNotFoundException
import no.nav.tsm.mottak.sykmelding.service.SykmeldingService
import no.nav.tsm.mottak.sykmelding.model.SykmeldingModule
import no.nav.tsm.mottak.sykmelding.model.SykmeldingRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class SykmeldingConsumer(
    private val sykmeldingService: SykmeldingService,
    @Value("\${nais.cluster}") private val clusterName: String
) {
    private val logger = LoggerFactory.getLogger(SykmeldingConsumer::class.java)

   @KafkaListener(
        topics = ["\${spring.kafka.topics.sykmeldinger-input}"],
        groupId = "regulus-maximus-consumer",
        containerFactory = "containerFactory",
        batch = "false"
    )
    fun consume(record: ConsumerRecord<String, SykmeldingRecord>) {
        try {
            sykmeldingService.updateSykmelding(record.key(), record.value())
        } catch (e: PersonNotFoundException) {
            logger.error("Failed to process sykmelding with id ${record.key()}", e)
            if(clusterName == "dev-gcp") {
                logger.warn("Person not found in dev-gcp, skipping sykmelding")
            } else {
                throw e
            }
        } catch (e: SykmeldingDBMappingException) {
            logger.error("Failed to process sykmelding with id ${record.key()}", e)
            if(clusterName == "dev-gcp") {
                logger.warn("Failed to map sykmelding in dev-gcp, skipping sykmelding")
            } else {
                throw e
            }
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
