package no.nav.tsm.mottak.sykmelding.kafka

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.Duration
import java.util.*
import kotlin.time.toJavaDuration
import no.nav.tsm.core.Environment
import no.nav.tsm.core.logger
import no.nav.tsm.sykmelding.input.core.model.SykmeldingModule
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer

class SykmeldingInputConsumer(environment: Environment) {
    private val logger = logger()

    private val topicName = "tsm.sykmeldinger"
    private val groupId = "regulus-maximus-consumer"

    private val duration: Duration =
        environment.kafka.sykmeldingInputConsumer.longPoll.toJavaDuration()
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

    fun poll(): List<Triple<String, SykmeldingRecord?, Headers>> {
        val records = consumer.poll(duration)
        if (records.isEmpty) return emptyList()

        logger.debug("Sykmelding consumer polled ${records.count()} records from $topicName")
        return records.map { record ->
            Triple(
                record.key(),
                record.value()?.let { parseAndMapSykmelding(it) },
                record.headers(),
            )
        }
    }

    fun subscribe() {
        logger.info("Subscribing $topicName")
        consumer.subscribe(listOf(topicName))
    }

    fun unsubscribe() {
        logger.info("Unsubscribing $topicName")
        consumer.unsubscribe()
    }

    private fun parseAndMapSykmelding(bytes: ByteArray): SykmeldingRecord {
        return recordObjectMapper.readValue<SykmeldingRecord>(bytes)
    }

    private val recordObjectMapper =
        jacksonObjectMapper().apply {
            registerModule(SykmeldingModule())
            registerModule(JavaTimeModule())
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }
}
