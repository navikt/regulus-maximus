package no.nav.tsm.mottak.sykmelding.kafka

import java.time.Duration
import java.util.*
import kotlin.time.toJavaDuration
import no.nav.tsm.core.Environment
import no.nav.tsm.ktor.logger
import no.nav.tsm.sykmelding.input.core.model.SykmeldingModule
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.module.kotlin.readValue

class SykmeldingInputConsumer(environment: Environment) {
    private val logger = logger()

    private val topicName = "tsm.sykmeldinger-input"
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

    private fun parseAndMapSykmelding(bytes: ByteArray): SykmeldingRecord? {
        return recordObjectMapper.readValue<SykmeldingRecord?>(bytes)
    }

    private val recordObjectMapper = jacksonMapperBuilder().addModules(SykmeldingModule()).build()
}
