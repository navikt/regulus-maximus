package no.nav.tsm.admin.datefixer

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.util.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import no.nav.tsm.core.Environment
import no.nav.tsm.core.RuntimeEnvironments
import no.nav.tsm.core.logger
import no.nav.tsm.sykmelding.input.core.model.SykmeldingModule
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer

class SykmeldingInputDateFixerConsumer(private val environment: Environment) {
    private val logger = logger()

    private val topicName = "tsm.sykmeldinger-input"
    private val groupId = "regulus-maximus-date-fixer-consumer"

    private val consumer: KafkaConsumer<String, ByteArray?>

    init {
        val kafkaProperties = Properties(environment.kafka.config)

        kafkaProperties.apply {
            this[ConsumerConfig.GROUP_ID_CONFIG] = groupId
            this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
            this[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"
        }

        consumer = KafkaConsumer(kafkaProperties, StringDeserializer(), ByteArrayDeserializer())
    }

    fun poll(): List<Pair<String, SykmeldingRecord?>> {
        val records = consumer.poll(10.seconds.toJavaDuration())
        if (records.isEmpty) return emptyList()

        logger.debug("Sykmelding consumer polled ${records.count()} records from $topicName")
        return records
            .mapNotNull {
                val value = it.value()
                if (value == null) null else it.key() to value
            }
            .map { (key, value) -> key to parseAndMapSykmelding(value) }
    }

    fun subscribe() {
        logger.info("Subscribing $topicName")
        consumer.subscribe(listOf(topicName))
    }

    fun unsubscribe() {
        logger.info("Unsubscribing $topicName")
        consumer.unsubscribe()
    }

    private fun parseAndMapSykmelding(bytes: ByteArray): SykmeldingRecord? {
        try {
            return recordObjectMapper.readValue<SykmeldingRecord>(bytes)
        } catch (ex: Exception) {
            if (environment.runtime.env != RuntimeEnvironments.PROD) {
                logger.warn(
                    "We're in ${environment.runtime.env}, ignoring parsing error. Its probably old garbasje."
                )
                return null
            }

            logger.error("Failed to parse SykmeldingRecord from bytes", ex)
            throw ex
        }
    }

    private val recordObjectMapper =
        jacksonObjectMapper().apply {
            registerModule(SykmeldingModule())
            registerModule(JavaTimeModule())
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }
}
