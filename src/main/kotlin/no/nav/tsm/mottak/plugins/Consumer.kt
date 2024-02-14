package no.nav.tsm.mottak.plugins

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import no.nav.tsm.mottak.sykmelding.SykmeldingConsumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer

import java.util.Properties

fun configureConsumer() {
    val props = Properties()
    props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = "localhost:9092"
    props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
    props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
    props[ConsumerConfig.GROUP_ID_CONFIG] = "regulus-maximus"
    val consumer = KafkaConsumer<String, String>(props)

    GlobalScope.launch(Dispatchers.IO) { SykmeldingConsumer(consumer).consumeSykmelding() }
}