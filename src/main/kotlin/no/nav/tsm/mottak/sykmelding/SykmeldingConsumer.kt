package no.nav.tsm.mottak.sykmelding

import no.nav.tsm.mottak.example.ExampleService
import no.nav.tsm.mottak.example.ExposedExample
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.koin.java.KoinJavaComponent.inject

import java.time.Duration

class SykmeldingConsumer(val kafkaConsumer: KafkaConsumer<String, String>) {
    val exampleService by inject<ExampleService>(clazz = ExampleService::class.java)
    suspend fun consumeSykmelding() {
        kafkaConsumer.subscribe(listOf("sykmelding-input"))
        while (true) {
            val records = kafkaConsumer.poll(Duration.ofMillis(100))
            records.forEach { record ->
                exampleService.create(
                    ExposedExample(
                        text = record.value().toString(),
                        someNumber = (0..100).random()
                    )
                )
            }
        }
    }
}