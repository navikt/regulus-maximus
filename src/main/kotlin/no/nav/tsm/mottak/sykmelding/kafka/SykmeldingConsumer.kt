package no.nav.tsm.mottak.sykmelding.kafka

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import no.nav.tsm.mottak.example.ExampleService
import no.nav.tsm.mottak.example.ExposedExample
import no.nav.tsm.mottak.sykmelding.kafka.model.SykmeldingInput
import no.nav.tsm.mottak.sykmelding.kafka.model.SykmeldingMedUtfall
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeaders
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class SykmeldingConsumer(
    private val kafkaConsumer: KafkaConsumer<String, SykmeldingInput>,
    private val sykmeldingInputTopic: String,
    private val sykmeldingOutputTopic: String,
    private val kafkaProducer: KafkaProducer<String, SykmeldingMedUtfall>,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(SykmeldingConsumer::class.java)
    }

    suspend fun consumeSykmelding() = withContext(Dispatchers.IO) {
        subscribeToKafkaTopics()
        try {
            while (isActive) {
                processMessages()
            }
        } finally {
            logger.info("unsubscribing and closing kafka consumer")
            kafkaConsumer.unsubscribe()
            kafkaConsumer.close()
        }
    }

    private suspend fun processMessages() {
        try {
            val records = kafkaConsumer.poll(1.seconds.toJavaDuration())
            records.forEach { record ->
                processRecord(record)
            }
        } catch (ex: Exception) {
            println("Error processing messages: ${ex.message}")
            kafkaConsumer.unsubscribe()
            delay(1.seconds)
            subscribeToKafkaTopics()
        }
    }


    private suspend fun processRecord(record: ConsumerRecord<String, SykmeldingInput>) {
        logger.info("Received message from topic: ${record.topic()}")
        withContext(Dispatchers.IO) {
            kafkaProducer.send(
                ProducerRecord(
                    sykmeldingOutputTopic,
                    UUID.randomUUID().toString(),
                    finnBehandlingsutfall(record.value()),
                )
            ).get()
        }
    }

    private fun finnBehandlingsutfall(sykmeldingInput: SykmeldingInput): SykmeldingMedUtfall {
        return SykmeldingMedUtfall(
            sykmeldingInput,
            "OK"
        )
    }

    private fun subscribeToKafkaTopics() {
        kafkaConsumer.subscribe(listOf(sykmeldingInputTopic))
    }
}
