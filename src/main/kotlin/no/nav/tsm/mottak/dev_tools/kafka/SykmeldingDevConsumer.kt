package no.nav.tsm.mottak.dev_tools.kafka

import no.nav.tsm.mottak.example.ExampleService
import no.nav.tsm.mottak.example.ExposedExample
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.koin.java.KoinJavaComponent.inject

import java.time.Duration

class SykmeldingDevConsumer(val kafkaConsumer: KafkaConsumer<String, String>) {
    val exampleService by inject<ExampleService>(clazz = ExampleService::class.java)
    suspend fun consumeSykmelding() {
        kafkaConsumer.subscribe(listOf("sykmelding-input", "sykmelding-utfall"))
        while (true) {
            val records = kafkaConsumer.poll(Duration.ofMillis(100))
            records.forEach { record ->
                if (record.topic() == "sykmelding-input") {
                    exampleService.create(
                        ExposedExample(
                            text = record.value().toString(),
                            someNumber = (0..100).random()
                        )
                    )
                } else if (record.topic() == "sykmelding-utfall") {
                    exampleService.create(
                        ExposedExample(
                            text = record.value().toString(),
                            someNumber = (100..200).random()
                        )
                    )
                }
            }
        }
    }
}