package no.nav.tsm.mottak.dev_tools.kafka

import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import no.nav.tsm.mottak.env
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer

//class DevConsumer() {
//    fun Application.startConsumer() {
//        val props = environment.env.kafkaConfig
//        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
//        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
//        props[ConsumerConfig.GROUP_ID_CONFIG] = "regulus-maximus"
//        val consumer = KafkaConsumer<String, String>(props)
//
//        GlobalScope.launch(Dispatchers.IO) { SykmeldingDevConsumer(consumer).consumeSykmelding() }
//    }
//
//    fun startConsumer() {
//        TODO("Not yet implemented")
//    }
//}

fun Application.configureDevConsumer() {
    val props = environment.env.kafkaConfig
    props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
    props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
    props[ConsumerConfig.GROUP_ID_CONFIG] = "regulus-maximus"
    val consumer = KafkaConsumer<String, String>(props)

    GlobalScope.launch(Dispatchers.IO) { SykmeldingDevConsumer(consumer).consumeSykmelding() }
}