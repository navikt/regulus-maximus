package no.nav.tsm.mottak.dev_tools

import indexPageRoute
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties


fun Application.module() {
  log.warn("Configuring development plugins, if you see this in production, something is wrong")
  val props = Properties()
  props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = "localhost:9092"
  props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
  props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
  val kafkaProducer = KafkaProducer<String, String>(props)
  configureDevRoutes(kafkaProducer)
  // Configure dev kafka producer
}

fun Application.configureDevRoutes(kafkaProducer: KafkaProducer<String, String>) {
  routing { indexPageRoute(kafkaProducer) }
}
