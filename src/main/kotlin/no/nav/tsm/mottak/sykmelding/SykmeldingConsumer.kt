package no.nav.tsm.mottak.sykmelding

import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

class SykmeldingConsumer(val kafkaConsumer: KafkaConsumer<String, String>) {
    fun consumeSykmelding() {
        kafkaConsumer.subscribe(listOf("sykmelding-input", "sykmelding-utfall"))
        while(true) {
            val records = kafkaConsumer.poll(Duration.ofMillis(100))
            for (record in records) {
                println(record.value())
            }
        }
    }
}