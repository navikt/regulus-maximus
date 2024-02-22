package no.nav.tsm.mottak.sykmelding.kafka

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import no.nav.tsm.mottak.example.ExampleService
import no.nav.tsm.mottak.example.ExposedExample
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class SykmeldingConsumer(
    private val kafkaConsumer: KafkaConsumer<String, String>,
    private val exampleService: ExampleService,
    private val topic: String,
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
            val records = kafkaConsumer.poll(10.seconds.toJavaDuration())
            records.forEach { record ->
                processRecord(record)
            }
        } catch (ex: Exception) {
            println("Error processing messages: ${ex.message}")
            kafkaConsumer.unsubscribe()
            delay(60.seconds)
            subscribeToKafkaTopics()
        }
    }


    private suspend fun processRecord(record: ConsumerRecord<String, String>) {
        val type = record.headers().single { it.key() == "type" }.value().toString(Charsets.UTF_8)
        logger.info("Received message from topic: ${record.topic()}: type: $type")
        if (type == "sykmelding-input") {
            exampleService.create(
                ExposedExample(
                    text = record.value().toString(),
                    someNumber = (0..100).random()
                )
            )
        } else if (type == "sykmelding-utfall") {

            exampleService.create(
                ExposedExample(
                    text = record.value().toString(),
                    someNumber = (100..200).random()
                )
            )
        }
    }

    private fun subscribeToKafkaTopics() {
        kafkaConsumer.subscribe(listOf(topic))
    }
}
